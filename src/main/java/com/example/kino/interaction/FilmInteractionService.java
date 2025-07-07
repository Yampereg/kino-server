package com.example.kino.interaction;

import com.example.kino.film.Film;
import com.example.kino.film.FilmRepository;
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
    private final PreferenceUpdateService preferenceUpdateService;

    @Transactional
    public void likeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save like interaction
        interactionRepository.save(new UserFilmInteraction(user, film, true));

        // Update preferences in background
        preferenceUpdateService.updatePreferences(user, film, 1);
    }

    @Transactional
    public void dislikeFilm(User user, Integer filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new RuntimeException("Film not found"));

        // Save dislike interaction
        interactionRepository.save(new UserFilmInteraction(user, film, false));

        // Update preferences in background
        preferenceUpdateService.updatePreferences(user, film, -1);
    }
}
