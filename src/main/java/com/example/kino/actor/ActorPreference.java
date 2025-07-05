package com.example.kino.actor;

import com.example.kino.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActorPreference {

    @EmbeddedId
    private ActorPreferenceKey id;

    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("actorId")
    private Actor actor;

    private double affinityscore;
}
