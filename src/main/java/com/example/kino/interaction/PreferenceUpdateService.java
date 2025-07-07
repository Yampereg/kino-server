// File: src/main/java/com/example/kino/interaction/PreferenceUpdateService.java

package com.example.kino.interaction;

import com.example.kino.actor.*;
import com.example.kino.director.*;
import com.example.kino.genre.*;
import com.example.kino.tag.*;
import com.example.kino.film.Film;
import com.example.kino.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreferenceUpdateService {

    private final TagPreferenceRepository tagPrefRepo;
    private final GenrePreferenceRepository genrePrefRepo;
    private final ActorPreferenceRepository actorPrefRepo;
    private final DirectorPreferenceRepository directorPrefRepo;

    @Async
    public void updatePreferences(User user, Film film, int delta) {
        film.getTags().forEach(tag -> {
            tagPrefRepo.findByUserAndTag(user, tag).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + delta);
                    tagPrefRepo.save(pref);
                },
                () -> tagPrefRepo.save(TagPreference.builder()
                    .id(new TagPreferenceKey(user.getId(), tag.getId()))
                    .user(user).tag(tag).affinityscore(delta).build())
            );
        });

        film.getGenres().forEach(genre -> {
            genrePrefRepo.findByUserAndGenre(user, genre).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + delta);
                    genrePrefRepo.save(pref);
                },
                () -> genrePrefRepo.save(GenrePreference.builder()
                    .id(new GenrePreferenceKey(user.getId(), genre.getId()))
                    .user(user).genre(genre).affinityscore(delta).build())
            );
        });

        film.getActors().forEach(actor -> {
            actorPrefRepo.findByUserAndActor(user, actor).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + delta);
                    actorPrefRepo.save(pref);
                },
                () -> actorPrefRepo.save(ActorPreference.builder()
                    .id(new ActorPreferenceKey(user.getId(), actor.getId()))
                    .user(user).actor(actor).affinityscore(delta).build())
            );
        });

        film.getDirectors().forEach(director -> {
            directorPrefRepo.findByUserAndDirector(user, director).ifPresentOrElse(
                pref -> {
                    pref.setAffinityscore(pref.getAffinityscore() + delta);
                    directorPrefRepo.save(pref);
                },
                () -> directorPrefRepo.save(DirectorPreference.builder()
                    .id(new DirectorPreferenceKey(user.getId(), director.getId()))
                    .user(user).director(director).affinityscore(delta).build())
            );
        });
    }
}
