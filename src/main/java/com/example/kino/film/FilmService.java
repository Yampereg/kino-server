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
        System.out.println("Start recommendations");

        Map<Integer, Double> genrePrefs = genrePrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getGenre().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> tagPrefs = tagPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getTag().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> actorPrefs = actorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getActor().getId(), p -> p.getAffinityscore()));
        Map<Integer, Double> directorPrefs = directorPrefRepo.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getDirector().getId(), p -> p.getAffinityscore()));


        System.out.println("yeah");
        int page = 0;
        int size = 500;
        List<Map.Entry<Film, Double>> topRecommendations = new ArrayList<>();

        while (true) {
            Page<Film> filmPage = filmRepository.findUnseenFilmsPage(user, PageRequest.of(page, size));
            List<Film> films = filmPage.getContent();
            if (films.isEmpty()) break;

            Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

            Map<Integer, Set<Integer>> filmGenres = relationsFetcher.fetchFilmGenres(filmIds);
            Map<Integer, Set<Integer>> filmTags = relationsFetcher.fetchFilmTags(filmIds);
            Map<Integer, Set<Integer>> filmActors = relationsFetcher.fetchFilmActors(filmIds);
            Map<Integer, Set<Integer>> filmDirectors = relationsFetcher.fetchFilmDirectors(filmIds);

            for (Film film : films) {
                int id = film.getId();
                double score = 0;
                for (Integer genreId : filmGenres.getOrDefault(id, Collections.emptySet()))
                    score += genrePrefs.getOrDefault(genreId, 0.0);
                for (Integer tagId : filmTags.getOrDefault(id, Collections.emptySet()))
                    score += tagPrefs.getOrDefault(tagId, 0.0);
                for (Integer actorId : filmActors.getOrDefault(id, Collections.emptySet()))
                    score += actorPrefs.getOrDefault(actorId, 0.0);
                for (Integer directorId : filmDirectors.getOrDefault(id, Collections.emptySet()))
                    score += directorPrefs.getOrDefault(directorId, 0.0);

                if (score > 0) {
                    topRecommendations.add(Map.entry(film, score));
                }
            }

            topRecommendations.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            if (topRecommendations.size() > 3) {
                topRecommendations = topRecommendations.subList(0, 3);
                break;
            }

            if (!filmPage.hasNext()) break;
            page++;
        }

        return topRecommendations.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
