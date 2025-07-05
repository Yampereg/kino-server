package com.example.kino.genre;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    private Integer id;

    @Column(name = "genre", length = 25)
    private String name;
}
