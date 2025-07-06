package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.data.FilmRelationsFetcher;
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
    private final FilmRelationsFetcher filmRelationsFetcher;

    public List<Film> getRecommendations(User user) {
        System.out.println("Start recommendations");

        List<Film> candidateFilms = filmRepository.findUnseenFilms(user);
        System.out.println("Got " + candidateFilms.size() + " candidate films");

        if (candidateFilms.isEmpty()) return Collections.emptyList();

        Map<Integer, Film> idToFilm = candidateFilms.stream()
                .collect(Collectors.toMap(Film::getId, f -> f));

        Set<Integer> filmIds = idToFilm.keySet();

        Map<Integer, Set<Integer>> filmGenres = filmRelationsFetcher.fetchFilmGenres(filmIds);
        Map<Integer, Set<Integer>> filmTags = filmRelationsFetcher.fetchFilmTags(filmIds);
        Map<Integer, Set<Integer>> filmActors = filmRelationsFetcher.fetchFilmActors(filmIds);
        Map<Integer, Set<Integer>> filmDirectors = filmRelationsFetcher.fetchFilmDirectors(filmIds);

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

        List<Integer> filteredFilmIds = filmIds.stream()
                .filter(id -> intersects(filmGenres.get(id), preferredGenreIds)
                        || intersects(filmTags.get(id), preferredTagIds)
                        || intersects(filmActors.get(id), preferredActorIds)
                        || intersects(filmDirectors.get(id), preferredDirectorIds))
                .limit(1000)
                .collect(Collectors.toList());

        System.out.println("Filtered down to " + filteredFilmIds.size() + " films");

        Map<Integer, Double> genrePrefs = genrePrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> tagPrefs = tagPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> actorPrefs = actorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> directorPrefs = directorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore()));

        List<FilmWithMetadata> metadataList = filteredFilmIds.stream()
                .map(id -> new FilmWithMetadata(
                        id,
                        filmGenres.getOrDefault(id, Set.of()),
                        filmTags.getOrDefault(id, Set.of()),
                        filmActors.getOrDefault(id, Set.of()),
                        filmDirectors.getOrDefault(id, Set.of())
                ))
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

    private boolean intersects(Set<Integer> a, Set<Integer> b) {
        if (a == null || b == null) return false;
        for (Integer x : a) if (b.contains(x)) return true;
        return false;
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
