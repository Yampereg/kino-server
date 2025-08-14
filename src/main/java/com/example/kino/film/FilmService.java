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

    public List<Film> getRecommendations(User user, int count) {
        Map<Integer, Double> genrePrefs = genrePrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> tagPrefs = tagPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> actorPrefs = actorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> directorPrefs = directorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore()));

        int page = 0;
        int size = 500;
        List<Map.Entry<Film, Double>> scoredFilms = new ArrayList<>();

        while (true) {
            Page<Film> filmPage = filmRepository.findUnseenFilmsPage(user, PageRequest.of(page, size));
            List<Film> films = filmPage.getContent();
            if (films.isEmpty()) break;

            Set<Integer> ids = films.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(ids);
            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(ids);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(ids);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(ids);

            for (Film f : films) {
                double score = scoreFilm(f, genrePrefs, tagPrefs, actorPrefs, directorPrefs,
                                        filmGenres, filmTags, filmActors, filmDirectors);
                if (score > 0) scoredFilms.add(Map.entry(f, score));
            }

            // Early stop if we have enough candidates
            if (scoredFilms.size() >= count * 5 || !filmPage.hasNext()) break;

            page++;
        }

        return scoredFilms.stream()
                .sorted(Map.Entry.<Film, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
