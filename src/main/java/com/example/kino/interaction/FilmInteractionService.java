// File: src/main/java/com/example/kino/interaction/FilmInteractionService.java
package com.example.kino.interaction;

import com.example.kino.actor.ActorPreference;
import com.example.kino.actor.ActorPreferenceKey;
import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.director.DirectorPreference;
import com.example.kino.director.DirectorPreferenceKey;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.film.Film;
import com.example.kino.film.FilmRepository;
import com.example.kino.genre.GenrePreference;
import com.example.kino.genre.GenrePreferenceKey;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.TagPreference;
import com.example.kino.tag.TagPreferenceKey;
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
        updateFilmInteraction(user, filmId, 1, true);
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        updateFilmInteraction(user, filmId, -1, false);
    }

    private void updateFilmInteraction(User user, Integer filmId, int affinityChange, boolean liked) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save interaction
        interactionRepository.save(new UserFilmInteraction(user, film, liked));

        // Tags
        film.getTags().forEach(tag -> {
            tagPrefRepo.findByUserAndTag(user, tag).ifPresentOrElse(
                pref -> tagPrefRepo.increment(user, tag.getId(), affinityChange),
                () -> {
                    TagPreference newPref = TagPreference.builder()
                            .id(new TagPreferenceKey(user.getId(), tag.getId()))
                            .user(user)
                            .tag(tag)
                            .affinityscore(affinityChange)
                            .build();
                    tagPrefRepo.save(newPref);
                }
            );
        });

        // Genres
        film.getGenres().forEach(genre -> {
            genrePrefRepo.findByUserAndGenre(user, genre).ifPresentOrElse(
                pref -> genrePrefRepo.increment(user, genre.getId(), affinityChange),
                () -> {
                    GenrePreference newPref = GenrePreference.builder()
                            .id(new GenrePreferenceKey(user.getId(), genre.getId()))
                            .user(user)
                            .genre(genre)
                            .affinityscore(affinityChange)
                            .build();
                    genrePrefRepo.save(newPref);
                }
            );
        });

        // Actors
        film.getActors().forEach(actor -> {
            actorPrefRepo.findByUserAndActor(user, actor).ifPresentOrElse(
                pref -> actorPrefRepo.increment(user, actor.getId(), affinityChange),
                () -> {
                    ActorPreference newPref = ActorPreference.builder()
                            .id(new ActorPreferenceKey(user.getId(), actor.getId()))
                            .user(user)
                            .actor(actor)
                            .affinityscore(affinityChange)
                            .build();
                    actorPrefRepo.save(newPref);
                }
            );
        });

        // Directors
        film.getDirectors().forEach(director -> {
            directorPrefRepo.findByUserAndDirector(user, director).ifPresentOrElse(
                pref -> directorPrefRepo.increment(user, director.getId(), affinityChange),
                () -> {
                    DirectorPreference newPref = DirectorPreference.builder()
                            .id(new DirectorPreferenceKey(user.getId(), director.getId()))
                            .user(user)
                            .director(director)
                            .affinityscore(affinityChange)
                            .build();
                    directorPrefRepo.save(newPref);
                }
            );
        });
    }
}
