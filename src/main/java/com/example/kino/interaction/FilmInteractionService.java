package com.example.kino.interaction;

import com.example.kino.actor.Actor;
import com.example.kino.actor.ActorPreference;
import com.example.kino.actor.ActorPreferenceKey;
import com.example.kino.actor.ActorPreferenceRepository;
import com.example.kino.director.Director;
import com.example.kino.director.DirectorPreference;
import com.example.kino.director.DirectorPreferenceKey;
import com.example.kino.director.DirectorPreferenceRepository;
import com.example.kino.film.Film;
import com.example.kino.film.FilmRepository;
import com.example.kino.genre.Genre;
import com.example.kino.genre.GenrePreference;
import com.example.kino.genre.GenrePreferenceKey;
import com.example.kino.genre.GenrePreferenceRepository;
import com.example.kino.tag.Tag;
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
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save like interaction
        interactionRepository.save(new UserFilmInteraction(user, film, true));

        // Update preferences or create if not exists
        film.getTags().forEach(tag -> {
            tagPrefRepo.findByUserAndTag(user, tag).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + 1);
                    tagPrefRepo.save(pref);
                },
                () -> {
                    TagPreference newPref = TagPreference.builder()
                            .id(new TagPreferenceKey(user.getId(), tag.getId()))
                            .user(user)
                            .tag(tag)
                            .affinityscore(1)
                            .build();
                    tagPrefRepo.save(newPref);
                }
            );
        });

        film.getGenres().forEach(genre -> {
            genrePrefRepo.findByUserAndGenre(user, genre).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + 1);
                    genrePrefRepo.save(pref);
                },
                () -> {
                    GenrePreference newPref = GenrePreference.builder()
                            .id(new GenrePreferenceKey(user.getId(), genre.getId()))
                            .user(user)
                            .genre(genre)
                            .affinityscore(1)
                            .build();
                    genrePrefRepo.save(newPref);
                }
            );
        });

        film.getActors().forEach(actor -> {
            actorPrefRepo.findByUserAndActor(user, actor).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + 1);
                    actorPrefRepo.save(pref);
                },
                () -> {
                    ActorPreference newPref = ActorPreference.builder()
                            .id(new ActorPreferenceKey(user.getId(), actor.getId()))
                            .user(user)
                            .actor(actor)
                            .affinityscore(1)
                            .build();
                    actorPrefRepo.save(newPref);
                }
            );
        });

        film.getDirectors().forEach(director -> {
            directorPrefRepo.findByUserAndDirector(user, director).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + 1);
                    directorPrefRepo.save(pref);
                },
                () -> {
                    DirectorPreference newPref = DirectorPreference.builder()
                            .id(new DirectorPreferenceKey(user.getId(), director.getId()))
                            .user(user)
                            .director(director)
                            .affinityscore(1)
                            .build();
                    directorPrefRepo.save(newPref);
                }
            );
        });
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save dislike interaction
        interactionRepository.save(new UserFilmInteraction(user, film, false));

        // Decrease preferences or create if not exists with -1 affinityscore
        film.getTags().forEach(tag -> {
            tagPrefRepo.findByUserAndTag(user, tag).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() - 1);
                    tagPrefRepo.save(pref);
                },
                () -> {
                    TagPreference newPref = TagPreference.builder()
                            .id(new TagPreferenceKey(user.getId(), tag.getId()))
                            .user(user)
                            .tag(tag)
                            .affinityscore(-1)
                            .build();
                    tagPrefRepo.save(newPref);
                }
            );
        });

        film.getGenres().forEach(genre -> {
            genrePrefRepo.findByUserAndGenre(user, genre).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() - 1);
                    genrePrefRepo.save(pref);
                },
                () -> {
                    GenrePreference newPref = GenrePreference.builder()
                            .id(new GenrePreferenceKey(user.getId(), genre.getId()))
                            .user(user)
                            .genre(genre)
                            .affinityscore(-1)
                            .build();
                    genrePrefRepo.save(newPref);
                }
            );
        });

        film.getActors().forEach(actor -> {
            actorPrefRepo.findByUserAndActor(user, actor).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() - 1);
                    actorPrefRepo.save(pref);
                },
                () -> {
                    ActorPreference newPref = ActorPreference.builder()
                            .id(new ActorPreferenceKey(user.getId(), actor.getId()))
                            .user(user)
                            .actor(actor)
                            .affinityscore(-1)
                            .build();
                    actorPrefRepo.save(newPref);
                }
            );
        });

        film.getDirectors().forEach(director -> {
            directorPrefRepo.findByUserAndDirector(user, director).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() - 1);
                    directorPrefRepo.save(pref);
                },
                () -> {
                    DirectorPreference newPref = DirectorPreference.builder()
                            .id(new DirectorPreferenceKey(user.getId(), director.getId()))
                            .user(user)
                            .director(director)
                            .affinityscore(-1)
                            .build();
                    directorPrefRepo.save(newPref);
                }
            );
        });
    }
}
