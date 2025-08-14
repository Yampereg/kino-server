// File: src/main/java/com/example/kino/film/FilmService.java
package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.data.FilmRelationsFetcher;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    public List<Film> getRecommendations(User user) {
        return getRecommendations(user, 10);
    }

    public List<Film> getRecommendations(User user, int count) {
        try {
            System.out.println("Start recommendations for user id=" + user.getId());

            Map<Integer, Double> genrePrefs = safeMap(genrePrefRepo.findByUser(user).stream()
                    .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore())));
            Map<Integer, Double> tagPrefs = safeMap(tagPrefRepo.findByUser(user).stream()
                    .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore())));
            Map<Integer, Double> actorPrefs = safeMap(actorPrefRepo.findByUser(user).stream()
                    .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore())));
            Map<Integer, Double> directorPrefs = safeMap(directorPrefRepo.findByUser(user).stream()
                    .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore())));

            System.out.println("Genre prefs: " + genrePrefs);
            System.out.println("Tag prefs: " + tagPrefs);
            System.out.println("Actor prefs: " + actorPrefs);
            System.out.println("Director prefs: " + directorPrefs);

            boolean hasPrefs = !genrePrefs.isEmpty() || !tagPrefs.isEmpty() || !actorPrefs.isEmpty() || !directorPrefs.isEmpty();

            int page = 0;
            int size = 500;
            List<Map.Entry<Film, Double>> scoredFilms = new ArrayList<>();

            while (true) {
                Page<Film> filmPage = filmRepository.findUnseenFilmsPage(user, PageRequest.of(page, size));
                List<Film> films = filmPage.getContent();
                if (films.isEmpty()) break;

                if (hasPrefs) {
                    Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());
                    Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
                    Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
                    Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);
                    Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);

                    for (Film film : films) {
                        double score = scoreFilm(film, genrePrefs, tagPrefs, actorPrefs, directorPrefs,
                                filmGenres, filmTags, filmActors, filmDirectors);
                        scoredFilms.add(Map.entry(film, score));
                    }
                } else {
                    // No preferences → fallback to popularity
                    for (Film film : films) {
                        scoredFilms.add(Map.entry(film, film.getPopularity())); // Assuming Film has getPopularity()
                    }
                }

                if (scoredFilms.size() >= count * 5 || !filmPage.hasNext()) break;
                page++;
            }

            return scoredFilms.stream()
                    .sorted(Map.Entry.<Film, Double>comparingByValue().reversed())
                    .limit(count)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error generating recommendations for user id=" + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Film> getNextToSwipe(User user) {
        return getRecommendations(user, 3);
    }

    private double scoreFilm(
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
        return filmGenres.getOrDefault(id, Set.of()).stream()
                .mapToDouble(gid -> genrePrefs.getOrDefault(gid, 0.0)).sum()
             + filmTags.getOrDefault(id, Set.of()).stream()
                .mapToDouble(tid -> tagPrefs.getOrDefault(tid, 0.0)).sum()
             + filmActors.getOrDefault(id, Set.of()).stream()
                .mapToDouble(aid -> actorPrefs.getOrDefault(aid, 0.0)).sum()
             + filmDirectors.getOrDefault(id, Set.of()).stream()
                .mapToDouble(did -> directorPrefs.getOrDefault(did, 0.0)).sum();
    }

    private Map<Integer, Double> safeMap(Map<Integer, Double> map) {
        return map != null ? map : Collections.emptyMap();
    }
}
