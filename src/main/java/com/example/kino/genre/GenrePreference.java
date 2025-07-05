package com.example.kino.genre;

import com.example.kino.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenrePreference {

    @EmbeddedId
    private GenrePreferenceKey id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("genreId")
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @Column(name = "affinityscore")
    private double affinityscore;
}
