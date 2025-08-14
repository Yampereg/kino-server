package com.example.kino.film;

import com.example.kino.auth.JwtService;
import com.example.kino.user.User;
import com.example.kino.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmRepository filmRepository;
    private final FilmService filmService;
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

    @GetMapping
    public List<Film> getAllFilms() {
        return filmRepository.findAll();
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Integer id) {
        return filmRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Film not found"));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<Film>> getRecommendations(@RequestHeader("Authorization") String authHeader) {
        User user = extractUserFromToken(authHeader);
        List<Film> recommendations = filmService.getRecommendations(user, 10);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/next")
    public ResponseEntity<List<Film>> getNextToSwipe(@RequestHeader("Authorization") String authHeader) {
        System.out.println("Auth header received: " + authHeader);

        User user = extractUserFromToken(authHeader);
        System.out.println("User extracted: " + user); // or user.getId(), user.getUsername()

        List<Film> nextFilms = filmService.getRecommendations(user, 3);
        return ResponseEntity.ok(nextFilms);
    }
}
