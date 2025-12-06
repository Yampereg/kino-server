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
            Map<Integer, Double> genrePrefs = getUserPreferences(genrePrefRepo.findByUser(user), p -> p.getGenre().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> tagPrefs = getUserPreferences(tagPrefRepo.findByUser(user), p -> p.getTag().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> actorPrefs = getUserPreferences(actorPrefRepo.findByUser(user), p -> p.getActor().getId(), p -> p.getAffinityscore());
            Map<Integer, Double> directorPrefs = getUserPreferences(directorPrefRepo.findByUser(user), p -> p.getDirector().getId(), p -> p.getAffinityscore());

            if (genrePrefs.isEmpty() && tagPrefs.isEmpty() && actorPrefs.isEmpty() && directorPrefs.isEmpty()) {
                return filmRepository.findTopPopularUnseen(user, PageRequest.of(0, count));
            }

            List<Film> candidates = filmRepository.findTopPopularUnseen(user, PageRequest.of(0, 200));
            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());

            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);

            return candidates.parallelStream()
                    .map(film -> {
                        double contentScore = calculateDeepScore(film, genrePrefs, tagPrefs, actorPrefs, directorPrefs, filmGenres, filmTags, filmActors, filmDirectors);
                        double popularityBoost = Math.log10(film.getPopularity() + 1) * 0.1;
                        return new AbstractMap.SimpleEntry<>(film, contentScore + popularityBoost);
                    })
                    .sorted(Map.Entry.<Film, Double>comparingByValue().reversed())
                    .limit(count)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return filmRepository.findTopPopularUnseen(user, PageRequest.of(0, count));
        }
    }

    public List<Film> getNextToSwipe(User user) {
        try {
            List<Film> candidates = filmRepository.findTopPopularUnseen(user, PageRequest.of(0, 50));
            
            Map<Integer, Double> genrePrefs = getUserPreferences(genrePrefRepo.findByUser(user), p -> p.getGenre().getId(), p -> p.getAffinityscore());

            if (genrePrefs.isEmpty()) {
                Collections.shuffle(candidates);
                return candidates.stream().limit(3).collect(Collectors.toList());
            }

            Integer favoriteGenreId = genrePrefs.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            Set<Integer> filmIds = candidates.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);

            return candidates.stream()
                    .sorted((f1, f2) -> {
                        boolean f1Match = filmGenres.getOrDefault(f1.getId(), Set.of()).contains(favoriteGenreId);
                        boolean f2Match = filmGenres.getOrDefault(f2.getId(), Set.of()).contains(favoriteGenreId);
                        if (f1Match && !f2Match) return -1;
                        if (!f1Match && f2Match) return 1;
                        return Double.compare(f2.getPopularity(), f1.getPopularity());
                    })
                    .limit(3)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return filmRepository.findTopPopularUnseen(user, PageRequest.of(0, 3));
        }
    }

    private double calculateDeepScore(
            Film film,
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

        score += filmGenres.getOrDefault(id, Set.of()).stream().mapToDouble(gid -> genrePrefs.getOrDefault(gid, 0.0)).sum() * 1.5;
        score += filmTags.getOrDefault(id, Set.of()).stream().mapToDouble(tid -> tagPrefs.getOrDefault(tid, 0.0)).sum() * 1.0;
        score += filmActors.getOrDefault(id, Set.of()).stream().mapToDouble(aid -> actorPrefs.getOrDefault(aid, 0.0)).sum() * 0.5;
        score += filmDirectors.getOrDefault(id, Set.of()).stream().mapToDouble(did -> directorPrefs.getOrDefault(did, 0.0)).sum() * 0.8;

        return score;
    }

    private <T> Map<Integer, Double> getUserPreferences(List<T> preferences, java.util.function.Function<T, Integer> idMapper, java.util.function.Function<T, Double> scoreMapper) {
        if (preferences == null) return Collections.emptyMap();
        return preferences.stream().collect(Collectors.toMap(idMapper, scoreMapper, (a, b) -> b));
    }
}