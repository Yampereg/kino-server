package com.example.kino.director;

import com.example.kino.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectorPreference {

    @EmbeddedId
    private DirectorPreferenceKey id;

    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("directorId")
    private Director director;

    private double affinityscore;
}
