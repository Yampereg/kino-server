package com.example.kino.director;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DirectorPreferenceKey implements Serializable {

    private Integer userId;
    private Integer directorId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DirectorPreferenceKey)) return false;
        DirectorPreferenceKey that = (DirectorPreferenceKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(directorId, that.directorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, directorId);
    }
}
