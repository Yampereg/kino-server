package com.example.kino.actor;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "actors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actor {

    @Id
    private Integer id;

    private Float popularity;

    @Column(name = "profile_path", length = 255)
    private String profilePath;

    @Column(length = 100)
    private String name;
}
