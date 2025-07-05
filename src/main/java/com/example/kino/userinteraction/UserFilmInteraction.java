package com.example.kino.userinteraction;

import com.example.kino.film.Film;
import com.example.kino.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_films_interaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilmInteraction {

    @EmbeddedId
    private UserFilmInteractionKey id;

    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("filmId")
    private Film film;

    private Boolean liked;

    public UserFilmInteraction(User user, Film film, Boolean liked) {
        this.user = user;
        this.film = film;
        this.liked = liked;
        this.id = new UserFilmInteractionKey(user.getId(), film.getId());
    }
}
