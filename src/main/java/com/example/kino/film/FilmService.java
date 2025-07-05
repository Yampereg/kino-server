// File: D:\kino-server\src\main\java\com\example\kino\film\FilmService.java

package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import lombok.RequiredArgsConstructor;
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

    public List<Film> getRecommendations(User user) {
        System.out.println("Start recommendations");

        List<Film> candidateFilms = filmRepository.findUnseenFilms(user);

        Set<Integer> preferredGenreIds = genrePrefRepo.findByUser(user).stream()
                .map(p -> p.getGenre().getId())
                .collect(Collectors.toSet());

        Set<Integer> preferredTagIds = tagPrefRepo.findByUser(user).stream()
                .map(p -> p.getTag().getId())
                .collect(Collectors.toSet());

        Set<Integer> preferredActorIds = actorPrefRepo.findByUser(user).stream()
                .map(p -> p.getActor().getId())
                .collect(Collectors.toSet());

        Set<Integer> preferredDirectorIds = directorPrefRepo.findByUser(user).stream()
                .map(p -> p.getDirector().getId())
                .collect(Collectors.toSet());

        List<Film> filteredFilms = candidateFilms.stream()
                .filter(film ->
                        film.getGenres().stream().anyMatch(g -> preferredGenreIds.contains(g.getId())) ||
                        film.getTags().stream().anyMatch(t -> preferredTagIds.contains(t.getId())) ||
                        film.getActors().stream().anyMatch(a -> preferredActorIds.contains(a.getId())) ||
                        film.getDirectors().stream().anyMatch(d -> preferredDirectorIds.contains(d.getId()))
                )
                .limit(1000)
                .collect(Collectors.toList());

        Map<Integer, Double> genrePrefs = genrePrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> tagPrefs = tagPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> actorPrefs = actorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> directorPrefs = directorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore()));

        // Map film id -> film (fully initialized)
        Map<Integer, Film> idToFilm = new HashMap<>();
        List<FilmWithMetadata> metadataList = filteredFilms.stream()
                .map(film -> {
                    idToFilm.put(film.getId(), film);
                    return new FilmWithMetadata(
                            film.getId(),
                            film.getGenres().stream().map(g -> g.getId()).collect(Collectors.toSet()),
                            film.getTags().stream().map(t -> t.getId()).collect(Collectors.toSet()),
                            film.getActors().stream().map(a -> a.getId()).collect(Collectors.toSet()),
                            film.getDirectors().stream().map(d -> d.getId()).collect(Collectors.toSet())
                    );
                })
                .collect(Collectors.toList());

        return metadataList.parallelStream()
                .map(meta -> Map.entry(
                        meta.filmId(),
                        computeScore(meta, genrePrefs, tagPrefs, actorPrefs, directorPrefs)
                ))
                .sorted(Comparator.comparingDouble(Map.Entry<Integer, Double>::getValue).reversed())
                .limit(3)
                .map(entry -> idToFilm.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private double computeScore(
            FilmWithMetadata meta,
            Map<Integer, Double> genrePrefs,
            Map<Integer, Double> tagPrefs,
            Map<Integer, Double> actorPrefs,
            Map<Integer, Double> directorPrefs) {

        double score = 0;
        for (Integer id : meta.genres()) score += genrePrefs.getOrDefault(id, 0.0);
        for (Integer id : meta.tags()) score += tagPrefs.getOrDefault(id, 0.0);
        for (Integer id : meta.actors()) score += actorPrefs.getOrDefault(id, 0.0);
        for (Integer id : meta.directors()) score += directorPrefs.getOrDefault(id, 0.0);
        return score;
    }

    private record FilmWithMetadata(
            Integer filmId,
            Set<Integer> genres,
            Set<Integer> tags,
            Set<Integer> actors,
            Set<Integer> directors
    ) {}
}
