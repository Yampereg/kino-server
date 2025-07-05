package com.example.kino.genre;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenrePreferenceKey implements Serializable {

    private Integer userId;
    private Integer genreId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenrePreferenceKey)) return false;
        GenrePreferenceKey that = (GenrePreferenceKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(genreId, that.genreId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, genreId);
    }
}
