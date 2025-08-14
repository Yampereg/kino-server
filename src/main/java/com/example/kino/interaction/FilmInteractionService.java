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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FilmInteractionService {

    private final FilmRepository filmRepository;
    private final UserFilmInteractionRepository interactionRepository;
    private final TagPreferenceRepository tagPrefRepo;
    private final GenrePreferenceRepository genrePrefRepo;
    private final ActorPreferenceRepository actorPrefRepo;
    private final DirectorPreferenceRepository directorPrefRepo;

    // per-user locks to avoid deadlocks
    private final Map<Integer, Object> userLocks = new ConcurrentHashMap<>();

    @Transactional
    public void likeFilm(User user, Integer filmId) {
        processFilmWithLock(user, filmId, 1, true);
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        processFilmWithLock(user, filmId, -1, false);
    }

    @Transactional
    public void superlikeFilm(User user, Integer filmId) {
        processFilmWithLock(user, filmId, 2, true);
    }

    private void processFilmWithLock(User user, Integer filmId, int affinityChange, boolean positiveInteraction) {
        Object lock = userLocks.computeIfAbsent(user.getId(), k -> new Object());
        synchronized (lock) {
            processFilm(user, filmId, affinityChange, positiveInteraction);
        }
    }

    private void processFilm(User user, Integer filmId, int affinityChange, boolean positiveInteraction) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        interactionRepository.save(new UserFilmInteraction(user, film, positiveInteraction));

        // tags
        for (Tag tag : film.getTags()) {
            tagPrefRepo.increment(user, tag.getId(), affinityChange);
        }
        // genres
        for (Genre genre : film.getGenres()) {
            genrePrefRepo.increment(user, genre.getId(), affinityChange);
        }
        // actors
        for (Actor actor : film.getActors()) {
            actorPrefRepo.increment(user, actor.getId(), affinityChange);
        }
        // directors
        for (Director director : film.getDirectors()) {
            directorPrefRepo.increment(user, director.getId(), affinityChange);
        }
    }
}
