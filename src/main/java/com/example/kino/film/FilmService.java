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

    private static final double TAG_WEIGHT = 10.0;
    private static final double GENRE_WEIGHT = 5.0;
    private static final double DIRECTOR_WEIGHT = 3.0;
    private static final double ACTOR_WEIGHT = 1.0;

    public List<Film> getRecommendations(User user, int count) {
        try {
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 2000));
            if (candidates.isEmpty()) return Collections.emptyList();

            var userTags = tagPrefRepo.findByUser(user);
            var userGenres = genrePrefRepo.findByUser(user);
            var userDirectors = directorPrefRepo.findByUser(user);
            var userActors = actorPrefRepo.findByUser(user);

            if (userTags.isEmpty() && userGenres.isEmpty() && userDirectors.isEmpty() && userActors.isEmpty()) {
                System.out.println("----- DEBUG: COLD START (No Prefs) -----");
                return candidates.stream().limit(count).collect(Collectors.toList());
            }

            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());

            Map<Integer, Double> tagScores = getScores(userTags, p -> p.getTag().getId(), p -> p.getAffinityscore());
            Map<Integer, String> tagNames = getNames(userTags, p -> p.getTag().getId(), p -> p.getTag().getName());

            Map<Integer, Double> genreScores = getScores(userGenres, p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Map<Integer, String> genreNames = getNames(userGenres, p -> p.getGenre().getId(), p -> p.getGenre().getName());

            Map<Integer, Double> dirScores = getScores(userDirectors, p -> p.getDirector().getId(), p -> p.getAffinityscore());
            Map<Integer, String> dirNames = getNames(userDirectors, p -> p.getDirector().getId(), p -> p.getDirector().getName());

            Map<Integer, Double> actorScores = getScores(userActors, p -> p.getActor().getId(), p -> p.getAffinityscore());
            Map<Integer, String> actorNames = getNames(userActors, p -> p.getActor().getId(), p -> p.getActor().getName());

            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);

            List<ScoredFilm> allScored = candidates.parallelStream()
                    .map(film -> calculateScore(
                            film,
                            tagScores, tagNames, filmTags.getOrDefault(film.getId(), Set.of()),
                            genreScores, genreNames, filmGenres.getOrDefault(film.getId(), Set.of()),
                            dirScores, dirNames, filmDirectors.getOrDefault(film.getId(), Set.of()),
                            actorScores, actorNames, filmActors.getOrDefault(film.getId(), Set.of())
                    ))
                    .collect(Collectors.toList());

            List<ScoredFilm> finalSelection = allScored.stream()
                    .filter(sf -> sf.getScore() > 0)
                    .sorted(Comparator.comparingDouble(ScoredFilm::getScore).reversed())
                    .limit(count)
                    .collect(Collectors.toList());

            System.out.println("----- SMART RECOMMENDATION LOG FOR USER " + user.getId() + " -----");
            for (ScoredFilm sf : finalSelection) {
                System.out.println("MATCH: " + sf.getFilm().getTitle() + " | SCORE: " + String.format("%.2f", sf.getScore()));
                System.out.println("   -> " + sf.getDebugNote());
            }

            if (finalSelection.size() < count) {
                System.out.println("   -> MATCHES EXHAUSTED. EXECUTING SMART RESCUE.");
                
                Integer topGenreId = genreScores.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
                
                String topGenreName = (topGenreId != null) ? genreNames.get(topGenreId) : "Unknown";

                if (topGenreId != null) {
                    Set<Integer> usedIds = finalSelection.stream().map(s -> s.getFilm().getId()).collect(Collectors.toSet());
                    
                    List<ScoredFilm> rescuedFilms = allScored.stream()
                            .filter(sf -> sf.getScore() == 0) 
                            .filter(sf -> !usedIds.contains(sf.getFilm().getId()))
                            .filter(sf -> filmGenres.getOrDefault(sf.getFilm().getId(), Set.of()).contains(topGenreId))
                            .sorted((s1, s2) -> Double.compare(s2.getFilm().getPopularity(), s1.getFilm().getPopularity()))
                            .limit(count - finalSelection.size())
                            .peek(sf -> sf.setDebugNote("Smart Rescue: Matches your favorite Genre [" + topGenreName + "]"))
                            .collect(Collectors.toList());
                    
                    for (ScoredFilm rescued : rescuedFilms) {
                        finalSelection.add(rescued);
                        System.out.println("RESCUED: " + rescued.getFilm().getTitle() + " (Genre: " + topGenreName + ")");
                    }
                }
            }
            System.out.println("---------------------------------------------------------------");

            return finalSelection.stream()
                    .map(ScoredFilm::getFilm)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Film> getNextToSwipe(User user) {
        try {
            List<Film> candidates = filmRepository.findCandidatesPool(user, PageRequest.of(0, 100));
            
            Map<Integer, Double> genrePrefs = getScores(genrePrefRepo.findByUser(user), p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Integer favoriteGenreId = genrePrefs.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            List<Film> priority = new ArrayList<>();
            List<Film> others = new ArrayList<>();

            if (favoriteGenreId != null) {
                Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());
                Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);

                for (Film f : candidates) {
                    Set<Integer> genres = filmGenres.getOrDefault(f.getId(), Set.of());
                    if (genres.contains(favoriteGenreId)) priority.add(f);
                    else others.add(f);
                }
            } else {
                others.addAll(candidates);
            }

            Collections.shuffle(priority);
            Collections.shuffle(others);

            List<Film> result = new ArrayList<>(priority);
            result.addAll(others);

            return result.stream().limit(3).collect(Collectors.toList());

        } catch (Exception e) {
            return filmRepository.findCandidatesPool(user, PageRequest.of(0, 3));
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

        for (Integer id : fTags) {
            if (tagPrefs.containsKey(id)) {
                double s = tagPrefs.get(id) * TAG_WEIGHT;
                score += s;
                note.append("[Tag: ").append(tagNames.get(id)).append("] ");
            }
        }

        for (Integer id : fGenres) {
            if (genrePrefs.containsKey(id)) {
                double s = genrePrefs.get(id) * GENRE_WEIGHT;
                score += s;
                note.append("[Genre: ").append(genreNames.get(id)).append("] ");
            }
        }

        for (Integer id : fDirs) {
            if (dirPrefs.containsKey(id)) {
                double s = dirPrefs.get(id) * DIRECTOR_WEIGHT;
                score += s;
                note.append("[Dir: ").append(dirNames.get(id)).append("] ");
            }
        }

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