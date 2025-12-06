package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.data.FilmRelationsFetcher;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmRepository filmRepository;
    private final GenrePreferenceRepository genrePrefRepo;
    private final TagPreferenceRepository tagPrefRepo;
    private final ActorPreferenceRepository actorPrefRepo;
    private final DirectorPreferenceRepository directorPrefRepo;
    private final FilmRelationsFetcher relationsFetcher;

    public List<Film> getRecommendations(User user, int count) {
        try {
            // 1. Load User Preferences
            Map<Integer, Double> genrePrefs = getUserPreferences(genrePrefRepo.findByUser(user), p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> tagPrefs = getUserPreferences(tagPrefRepo.findByUser(user), p -> p.getTag().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> actorPrefs = getUserPreferences(actorPrefRepo.findByUser(user), p -> p.getActor().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> directorPrefs = getUserPreferences(directorPrefRepo.findByUser(user), p -> p.getDirector().getId(), p -> p.getAffinityscore());

            // If no data, return generic popular films
            if (genrePrefs.isEmpty() && tagPrefs.isEmpty() && actorPrefs.isEmpty() && directorPrefs.isEmpty()) {
                return filmRepository.findCandidatesPool(user, PageRequest.of(0, count));
            }

            // 2. Identify "Power Genres" (The user's top 3 genres)
            Set<Integer> topGenreIds = genrePrefs.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // 3. Fetch Deep Candidate Pool (Top 1000 unseen films)
            // We cast a wide net to ensure we catch niche films that fit preferences, even if they aren't in the top 100.
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 1000));
            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());

            // 4. Batch Fetch Relations
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);

            // 5. Complex Scoring & Filtering
            List<Film> rankedFilms = candidates.parallelStream()
                    .map(film -> {
                        double score = calculatePureContentScore(
                                film, topGenreIds, 
                                genrePrefs, tagPrefs, actorPrefs, directorPrefs,
                                filmGenres, filmTags, filmActors, filmDirectors
                        );
                        return new AbstractMap.SimpleEntry<>(film, score);
                    })
                    // Strict Filter: Score must be > 1.0. This eliminates films with only weak/accidental associations.
                    .filter(entry -> entry.getValue() > 1.0) 
                    .sorted(Map.Entry.<Film, Double>comparingByValue().reversed())
                    .limit(count)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 6. Fallback: If strict matching returned too few results, fill with popular unseen
            if (rankedFilms.size() < count) {
                fillWithFallback(rankedFilms, candidates, count);
            }

            return rankedFilms;

        } catch (Exception e) {
            e.printStackTrace();
            return filmRepository.findCandidatesPool(user, PageRequest.of(0, count));
        }
    }

    public List<Film> getNextToSwipe(User user) {
        try {
            // Fast Lane: Smaller pool, simplified logic
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 50));
            
            // Just grab the absolute favorite genre
            Integer favoriteGenreId = genrePrefRepo.findByUser(user).stream()
                    .max(Comparator.comparingDouble(p -> p.getAffinityscore()))
                    .map(p -> p.getGenre().getId())
                    .orElse(null);

            if (favoriteGenreId == null) {
                Collections.shuffle(candidates);
                return candidates.stream().limit(3).collect(Collectors.toList());
            }

            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);

            // Sort: Films in favorite genre first, then by popularity
            return candidates.stream()
                    .sorted((f1, f2) -> {
                        boolean f1Match = filmGenres.getOrDefault(f1.getId(), Set.of()).contains(favoriteGenreId);
                        boolean f2Match = filmGenres.getOrDefault(f2.getId(), Set.of()).contains(favoriteGenreId);
                        
                        if (f1Match && !f2Match) return -1;
                        if (!f1Match && f2Match) return 1;
                        return 0; // Maintain original popularity order if both match or neither match
                    })
                    .limit(3)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return filmRepository.findCandidatesPool(user, PageRequest.of(0, 3));
        }
    }

    private double calculatePureContentScore(
            Film film,
            Set<Integer> topGenreIds,
            Map<Integer, Double> genrePrefs,
            Map<Integer, Double> tagPrefs,
            Map<Integer, Double> actorPrefs,
            Map<Integer, Double> directorPrefs,
            Map<Integer, Set<Integer>> filmGenres,
            Map<Integer, Set<Integer>> filmTags,
            Map<Integer, Set<Integer>> filmActors,
            Map<Integer, Set<Integer>> filmDirectors
    ) {
        int id = film.getId();
        double score = 0.0;
        
        // --- Weighted Sum Components ---
        // Genre: High impact (Weight 3.0)
        score += filmGenres.getOrDefault(id, Set.of()).stream()
                .mapToDouble(gid -> genrePrefs.getOrDefault(gid, 0.0) * 3.0)
                .sum();

        // Tags: Medium impact (Weight 2.0) - crucial for specific vibes (e.g., "Zombie")
        score += filmTags.getOrDefault(id, Set.of()).stream()
                .mapToDouble(tid -> tagPrefs.getOrDefault(tid, 0.0) * 2.0)
                .sum();

        // Directors: Medium-High impact (Weight 2.5) - Stylistic match
        score += filmDirectors.getOrDefault(id, Set.of()).stream()
                .mapToDouble(did -> directorPrefs.getOrDefault(did, 0.0) * 2.5)
                .sum();

        // Actors: Low impact (Weight 1.0) - Users often swipe for plot/genre, not just actors
        score += filmActors.getOrDefault(id, Set.of()).stream()
                .mapToDouble(aid -> actorPrefs.getOrDefault(aid, 0.0))
                .sum();

        // --- Synergy Multiplier ---
        // If the film belongs to one of the user's TOP 3 genres, boost the total score by 20%
        // This ensures a "Zombie" tag in a "Horror" movie counts more than a "Zombie" tag in a "Comedy"
        boolean isPowerGenre = filmGenres.getOrDefault(id, Set.of()).stream().anyMatch(topGenreIds::contains);
        if (isPowerGenre) {
            score *= 1.2;
        }

        return score;
    }

    private void fillWithFallback(List<Film> targetList, List<Film> sourceCandidates, int required) {
        Set<Integer> existingIds = targetList.stream().map(Film::getId).collect(Collectors.toSet());
        for (Film f : sourceCandidates) {
            if (targetList.size() >= required) break;
            if (!existingIds.contains(f.getId())) {
                targetList.add(f);
                existingIds.add(f.getId());
            }
        }
    }

    private <T> Map<Integer, Double> getUserPreferences(List<T> preferences, java.util.function.Function<T, Integer> idMapper, java.util.function.Function<T, Double> scoreMapper) {
        if (preferences == null) return Collections.emptyMap();
        return preferences.stream().collect(Collectors.toMap(idMapper, scoreMapper, (a, b) -> b));
    }
}