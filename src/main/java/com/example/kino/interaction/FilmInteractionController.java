package com.example.kino.interaction;

import com.example.kino.auth.JwtService;
import com.example.kino.film.Film;
import com.example.kino.user.User;
import com.example.kino.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class FilmInteractionController {

    private final FilmInteractionService filmInteractionService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private User extractUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/like/{filmId}")
    public ResponseEntity<String> likeFilm(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Integer filmId) {
        User user = extractUserFromToken(authHeader);
        filmInteractionService.likeFilm(user, filmId);
        return ResponseEntity.ok("Liked");
    }

    @PostMapping("/dislike/{filmId}")
    public ResponseEntity<String> dislikeFilm(@RequestHeader("Authorization") String authHeader,
                                              @PathVariable Integer filmId) {
        User user = extractUserFromToken(authHeader);
        filmInteractionService.dislikeFilm(user, filmId);
        return ResponseEntity.ok("Disliked");
    }

    @GetMapping("/liked")
    public ResponseEntity<List<Film>> getLikedFilms(@RequestHeader("Authorization") String authHeader) {
        User user = extractUserFromToken(authHeader);
        List<Film> likedFilms = filmInteractionService.getLikedFilms(user);
        return ResponseEntity.ok(likedFilms);
    }

    @GetMapping("/disliked")
    public ResponseEntity<List<Film>> getDislikedFilms(@RequestHeader("Authorization") String authHeader) {
        User user = extractUserFromToken(authHeader);
        List<Film> dislikedFilms = filmInteractionService.getDislikedFilms(user);
        return ResponseEntity.ok(dislikedFilms);
    }
}