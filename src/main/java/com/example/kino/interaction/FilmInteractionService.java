// File: src/main/java/com/example/kino/interaction/FilmInteractionService.java
package com.example.kino.interaction;

import com.example.kino.actor.Actor;
import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.director.Director;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.film.Film;
import com.example.kino.film.FilmRepository;
import com.example.kino.genre.Genre;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.Tag;
import com.example.kino.tag.TagPreferenceRepository;
import com.example.kino.user.User;
import com.example.kino.userinteraction.UserFilmInteraction;
import com.example.kino.userinteraction.UserFilmInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FilmInteractionService {

    private final FilmRepository filmRepository;
    private final UserFilmInteractionRepository interactionRepository;

    private final TagPreferenceRepository tagPrefRepo;
    private final GenrePreferenceRepository genrePrefRepo;
    private final ActorPreferenceRepository actorPrefRepo;
    private final DirectorPreferenceRepository directorPrefRepo;

    @Transactional
    public void likeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save like interaction
        interactionRepository.save(new UserFilmInteraction(user, film, true));

        // Atomic upserts for all preferences
        film.getTags().forEach(tag -> tagPrefRepo.upsert(user.getId(), tag.getId(), 1));
        film.getGenres().forEach(genre -> genrePrefRepo.upsert(user.getId(), genre.getId(), 1));
        film.getActors().forEach(actor -> actorPrefRepo.upsert(user.getId(), actor.getId(), 1));
        film.getDirectors().forEach(director -> directorPrefRepo.upsert(user.getId(), director.getId(), 1));
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save dislike interaction
        interactionRepository.save(new UserFilmInteraction(user, film, false));

        // Atomic upserts for all preferences with negative score
        film.getTags().forEach(tag -> tagPrefRepo.upsert(user.getId(), tag.getId(), -1));
        film.getGenres().forEach(genre -> genrePrefRepo.upsert(user.getId(), genre.getId(), -1));
        film.getActors().forEach(actor -> actorPrefRepo.upsert(user.getId(), actor.getId(), -1));
        film.getDirectors().forEach(director -> directorPrefRepo.upsert(user.getId(), director.getId(), -1));
    }

    @Transactional
    public void superlikeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save superlike as positive interaction
        interactionRepository.save(new UserFilmInteraction(user, film, true));

        // Atomic upserts with higher score for superlike
        film.getTags().forEach(tag -> tagPrefRepo.upsert(user.getId(), tag.getId(), 2));
        film.getGenres().forEach(genre -> genrePrefRepo.upsert(user.getId(), genre.getId(), 2));
        film.getActors().forEach(actor -> actorPrefRepo.upsert(user.getId(), actor.getId(), 2));
        film.getDirectors().forEach(director -> directorPrefRepo.upsert(user.getId(), director.getId(), 2));
    }
}
