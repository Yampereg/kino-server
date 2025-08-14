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

import java.util.Comparator;
import java.util.List;

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
                .orElseThrow(() -> new RuntimeException("Film not found: " + filmId));

        // Record the interaction first (idempotency depends on a unique constraint in this table if desired)
        interactionRepository.save(new UserFilmInteraction(user, film, positiveInteraction));

        // Update preferences in a deterministic order (by id) to avoid lock-order inversions
        List<Tag> tags = film.getTags().stream().sorted(Comparator.comparing(Tag::getId)).toList();
        for (Tag tag : tags) {
            tagPrefRepo.upsertIncrement(user.getId(), tag.getId(), affinityChange);
        }

        List<Genre> genres = film.getGenres().stream().sorted(Comparator.comparing(Genre::getId)).toList();
        for (Genre genre : genres) {
            genrePrefRepo.upsertIncrement(user.getId(), genre.getId(), affinityChange);
        }

        List<Actor> actors = film.getActors().stream().sorted(Comparator.comparing(Actor::getId)).toList();
        for (Actor actor : actors) {
            actorPrefRepo.upsertIncrement(user.getId(), actor.getId(), affinityChange);
        }

        List<Director> directors = film.getDirectors().stream().sorted(Comparator.comparing(Director::getId)).toList();
        for (Director director : directors) {
            directorPrefRepo.upsertIncrement(user.getId(), director.getId(), affinityChange);
        }
    }
}
