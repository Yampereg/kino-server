package com.example.kino.film;

import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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

    @PersistenceContext
    private EntityManager entityManager;

    public List<Film> getRecommendations(User user) {
        System.out.println("Start recommendations");

        List<Film> candidateFilms = filmRepository.findUnseenFilms(user);
        System.out.println("Got " + candidateFilms.size() + " candidate films");

        if (candidateFilms.isEmpty()) return List.of();

        Map<Integer, Film> idToFilm = candidateFilms.stream()
                .collect(Collectors.toMap(Film::getId, f -> f));

        List<Integer> filmIds = new ArrayList<>(idToFilm.keySet());

        // Fetch genres, tags, actors, directors in batch
        Map<Integer, Set<Integer>> filmGenres = fetchIdMap("SELECT fg.film.id, fg.genre.id FROM FilmGenre fg WHERE fg.film.id IN :filmIds", filmIds);
        Map<Integer, Set<Integer>> filmTags = fetchIdMap("SELECT ft.film.id, ft.tag.id FROM FilmTag ft WHERE ft.film.id IN :filmIds", filmIds);
        Map<Integer, Set<Integer>> filmActors = fetchIdMap("SELECT fa.film.id, fa.actor.id FROM FilmActor fa WHERE fa.film.id IN :filmIds", filmIds);
        Map<Integer, Set<Integer>> filmDirectors = fetchIdMap("SELECT fd.film.id, fd.director.id FROM FilmDirector fd WHERE fd.film.id IN :filmIds", filmIds);

        // Load user preferences
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

        // Filter films
        List<Integer> filteredFilmIds = filmIds.stream()
                .filter(id ->
                        intersects(filmGenres.get(id), preferredGenreIds)
                        || intersects(filmTags.get(id), preferredTagIds)
                        || intersects(filmActors.get(id), preferredActorIds)
                        || intersects(filmDirectors.get(id), preferredDirectorIds)
                )
                .limit(1000)
                .collect(Collectors.toList());

        System.out.println("Filtered down to " + filteredFilmIds.size() + " films");

        // Preference scores
        Map<Integer, Double> genrePrefs = genrePrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> tagPrefs = tagPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> actorPrefs = actorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore()));

        Map<Integer, Double> directorPrefs = directorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore()));

        // Metadata per film
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

    private Map<Integer, Set<Integer>> fetchIdMap(String queryStr, List<Integer> filmIds) {
        TypedQuery<Object[]> query = entityManager.createQuery(queryStr, Object[].class);
        query.setParameter("filmIds", filmIds);
        List<Object[]> rows = query.getResultList();
        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (Object[] row : rows) {
            Integer filmId = (Integer) row[0];
            Integer relatedId = (Integer) row[1];
            map.computeIfAbsent(filmId, k -> new HashSet<>()).add(relatedId);
        }
        return map;
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
