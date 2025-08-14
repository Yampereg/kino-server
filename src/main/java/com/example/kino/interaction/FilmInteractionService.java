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
        processFilm(user, filmId, 1, true);
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        processFilm(user, filmId, -1, false);
    }

    @Transactional
    public void superlikeFilm(User user, Integer filmId) {
        processFilm(user, filmId, 2, true);
    }

    private void processFilm(User user, Integer filmId, int affinityChange, boolean positiveInteraction) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        interactionRepository.save(new UserFilmInteraction(user, film, positiveInteraction));

        for (Tag tag : film.getTags()) {
            tagPrefRepo.incrementOrInsert(user.getId(), tag.getId(), affinityChange);
        }
        for (Genre genre : film.getGenres()) {
            genrePrefRepo.increment(user, genre.getId(), affinityChange);
        }
        for (Actor actor : film.getActors()) {
            actorPrefRepo.increment(user, actor.getId(), affinityChange);
        }
        for (Director director : film.getDirectors()) {
            directorPrefRepo.increment(user, director.getId(), affinityChange);
        }
    }
}
