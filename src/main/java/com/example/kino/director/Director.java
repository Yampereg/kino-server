package com.example.kino.director;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "directors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Director {

    @Id
    private Integer id;

    private Float popularity;

    @Column(name = "profile_path", length = 255)
    private String profilePath;

    @Column(length = 100)
    private String name;
}
