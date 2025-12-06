package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.data.FilmRelationsFetcher;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
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

    private static final double TAG_WEIGHT = 15.0;     
    private static final double GENRE_WEIGHT = 5.0;    
    private static final double DIRECTOR_WEIGHT = 3.0; 
    private static final double ACTOR_WEIGHT = 1.0;    

    public List<Film> getRecommendations(User user, int count) {
        try {
            // 1. Fetch a large pool of unseen films (Top 500 popular)
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 500));
            if (candidates.isEmpty()) return Collections.emptyList();

            // 2. Fetch User Preferences
            var userTags = tagPrefRepo.findByUser(user);
            var userGenres = genrePrefRepo.findByUser(user);
            var userDirectors = directorPrefRepo.findByUser(user);
            var userActors = actorPrefRepo.findByUser(user);

            // 3. Cold Start Check: If user has NO preferences, return pure random discovery
            boolean isColdStart = userTags.isEmpty() && userGenres.isEmpty() && userDirectors.isEmpty() && userActors.isEmpty();
            if (isColdStart) {
                System.out.println("----- DEBUG: COLD START (No Preferences) -----");
                Collections.shuffle(candidates); 
                return candidates.stream().limit(count).collect(Collectors.toList());
            }

            // 4. Map Preferences for fast lookup
            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, Double> tagScores = getScores(userTags, p -> p.getTag().getId(), p -> p.getAffinityscore());
            Map<Integer, String> tagNames = getNames(userTags, p -> p.getTag().getId(), p -> p.getTag().getName());

            Map<Integer, Double> genreScores = getScores(userGenres, p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Map<Integer, String> genreNames = getNames(userGenres, p -> p.getGenre().getId(), p -> p.getGenre().getName());

            Map<Integer, Double> dirScores = getScores(userDirectors, p -> p.getDirector().getId(), p -> p.getAffinityscore());
            Map<Integer, String> dirNames = getNames(userDirectors, p -> p.getDirector().getId(), p -> p.getDirector().getName());

            Map<Integer, Double> actorScores = getScores(userActors, p -> p.getActor().getId(), p -> p.getAffinityscore());
            Map<Integer, String> actorNames = getNames(userActors, p -> p.getActor().getId(), p -> p.getActor().getName());

            // 5. Fetch Relations for Candidates
            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);

            // 6. Calculate Scores (Additive Logic - OR condition)
            List<ScoredFilm> rankedParams = candidates.parallelStream()
                    .map(film -> calculateScore(
                            film,
                            tagScores, tagNames, filmTags.getOrDefault(film.getId(), Set.of()),
                            genreScores, genreNames, filmGenres.getOrDefault(film.getId(), Set.of()),
                            dirScores, dirNames, filmDirectors.getOrDefault(film.getId(), Set.of()),
                            actorScores, actorNames, filmActors.getOrDefault(film.getId(), Set.of())
                    ))
                    .filter(sf -> sf.getScore() > 0) 
                    .sorted(Comparator.comparingDouble(ScoredFilm::getScore).reversed())
                    .collect(Collectors.toList());

            // 7. Process Final Output
            System.out.println("----- RECOMMENDATION DEBUG LOG FOR USER " + user.getId() + " -----");
            
            List<Film> finalRecommendations = new ArrayList<>();
            Set<Integer> usedIds = new HashSet<>();

            // Take Matches
            for (ScoredFilm sf : rankedParams) {
                if (finalRecommendations.size() >= count) break;
                finalRecommendations.add(sf.getFilm());
                usedIds.add(sf.getFilm().getId());
                System.out.println("MATCH: " + sf.getFilm().getTitle() + " | SCORE: " + String.format("%.2f", sf.getScore()));
                System.out.println("   -> " + sf.getDebugNote());
            }

            // Fill with Random Fallbacks if not enough matches
            if (finalRecommendations.size() < count) {
                System.out.println("   -> NOT ENOUGH MATCHES. FILLING WITH RANDOMIZED POPULAR FILMS.");
                Collections.shuffle(candidates); // Randomize the pool so fallbacks differ each time
                
                for (Film f : candidates) {
                    if (finalRecommendations.size() >= count) break;
                    if (!usedIds.contains(f.getId())) {
                        finalRecommendations.add(f);
                        usedIds.add(f.getId());
                        System.out.println("RANDOM FALLBACK: " + f.getTitle());
                    }
                }
            }
            System.out.println("---------------------------------------------------------------");

            return finalRecommendations;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Film> getNextToSwipe(User user) {
        try {
            // 1. Get Top 100 Popular Unseen to ensure quality pool
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 100));
            
            // 2. Identify Favorite Genre
            Map<Integer, Double> genrePrefs = getScores(genrePrefRepo.findByUser(user), p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Integer favoriteGenreId = genrePrefs.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            List<Film> priorityFilms = new ArrayList<>();
            List<Film> otherFilms = new ArrayList<>();

            // 3. Split into Priority (Genre Match) and Others
            if (favoriteGenreId != null) {
                Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());
                Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);

                for (Film f : candidates) {
                    Set<Integer> genres = filmGenres.getOrDefault(f.getId(), Set.of());
                    if (genres.contains(favoriteGenreId)) {
                        priorityFilms.add(f);
                    } else {
                        otherFilms.add(f);
                    }
                }
            } else {
                otherFilms.addAll(candidates);
            }

            // 4. Shuffle both lists to ensure randomness on every call
            Collections.shuffle(priorityFilms);
            Collections.shuffle(otherFilms);

            // 5. Merge: Priority first, then others
            List<Film> result = new ArrayList<>(priorityFilms);
            result.addAll(otherFilms);

            return result.stream().limit(3).collect(Collectors.toList());

        } catch (Exception e) {
            // Fallback: Random 3 from simple pool
            List<Film> fallback = filmRepository.findCandidatesPool(user, PageRequest.of(0, 20));
            Collections.shuffle(fallback);
            return fallback.stream().limit(3).collect(Collectors.toList());
        }
    }

    private ScoredFilm calculateScore(
            Film film,
            Map<Integer, Double> tagPrefs, Map<Integer, String> tagNames, Set<Integer> fTags,
            Map<Integer, Double> genrePrefs, Map<Integer, String> genreNames, Set<Integer> fGenres,
            Map<Integer, Double> dirPrefs, Map<Integer, String> dirNames, Set<Integer> fDirs,
            Map<Integer, Double> actorPrefs, Map<Integer, String> actorNames, Set<Integer> fActors
    ) {
        double score = 0.0;
        StringBuilder note = new StringBuilder();

        // 1. Tags (Most Important)
        for (Integer id : fTags) {
            if (tagPrefs.containsKey(id)) {
                double s = tagPrefs.get(id) * TAG_WEIGHT;
                score += s;
                note.append("[Tag: ").append(tagNames.get(id)).append("] ");
            }
        }

        // 2. Genres (High)
        for (Integer id : fGenres) {
            if (genrePrefs.containsKey(id)) {
                double s = genrePrefs.get(id) * GENRE_WEIGHT;
                score += s;
                note.append("[Genre: ").append(genreNames.get(id)).append("] ");
            }
        }

        // 3. Directors (Medium)
        for (Integer id : fDirs) {
            if (dirPrefs.containsKey(id)) {
                double s = dirPrefs.get(id) * DIRECTOR_WEIGHT;
                score += s;
                note.append("[Dir: ").append(dirNames.get(id)).append("] ");
            }
        }

        // 4. Actors (Low)
        for (Integer id : fActors) {
            if (actorPrefs.containsKey(id)) {
                double s = actorPrefs.get(id) * ACTOR_WEIGHT;
                score += s;
                note.append("[Act: ").append(actorNames.get(id)).append("] ");
            }
        }

        return new ScoredFilm(film, score, note.toString());
    }

    private <T> Map<Integer, Double> getScores(List<T> list, java.util.function.Function<T, Integer> idMapper, java.util.function.Function<T, Double> valMapper) {
        if (list == null) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(idMapper, valMapper, (a, b) -> b));
    }

    private <T> Map<Integer, String> getNames(List<T> list, java.util.function.Function<T, Integer> idMapper, java.util.function.Function<T, String> nameMapper) {
        if (list == null) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(idMapper, nameMapper, (a, b) -> b));
    }

    @Data
    @AllArgsConstructor
    private static class ScoredFilm {
        private Film film;
        private double score;
        private String debugNote;
    }
}