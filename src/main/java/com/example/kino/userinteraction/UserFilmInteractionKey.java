package com.example.kino.userinteraction;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFilmInteractionKey implements Serializable {

    private Integer userId;
    private Integer filmId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserFilmInteractionKey)) return false;
        UserFilmInteractionKey that = (UserFilmInteractionKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(filmId, that.filmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, filmId);
    }
}
