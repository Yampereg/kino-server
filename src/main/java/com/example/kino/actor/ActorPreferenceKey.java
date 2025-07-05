package com.example.kino.actor;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActorPreferenceKey implements Serializable {

    private Integer userId;
    private Integer actorId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActorPreferenceKey)) return false;
        ActorPreferenceKey that = (ActorPreferenceKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(actorId, that.actorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, actorId);
    }
}
