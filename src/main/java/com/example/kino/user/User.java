package com.example.kino.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
    private Integer id;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 20)
    private String name;

    @Column(name = "passwordhash", length = 255)
    private String passwordHash;
}
