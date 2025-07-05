package com.example.kino.film;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.example.kino.genre.Genre;
import com.example.kino.tag.Tag;
import com.example.kino.director.Director;
import com.example.kino.actor.Actor;

@Entity
@Table(name = "films")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Film {

    @Id
    private Integer id;

    @Column(length = 255)
    private String collection;

    private Integer budget;

    @Column(name = "origin_country", columnDefinition = "text[]")
    private List<String> originCountry;

    @Column(name = "origin_language", length = 50)
    private String originLanguage;

    @Column(length = 255)
    private String title;

    @Column(length = 1000)
    private String overview;

    private Float popularity;

    @Column(name = "poster_path", length = 255)
    private String posterPath;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    private Integer runtime;

    @Column(name = "vote_average")
    private Float voteAverage;

    @Column(name = "banner_path", length = 255)
    private String bannerPath;

    @ManyToMany
    @JoinTable(
            name = "filmgenres",
            joinColumns = @JoinColumn(name = "filmid"),
            inverseJoinColumns = @JoinColumn(name = "genreid")
    )
    private Set<Genre> genres;

    @ManyToMany
    @JoinTable(
            name = "filmdirectors",
            joinColumns = @JoinColumn(name = "filmid"),
            inverseJoinColumns = @JoinColumn(name = "directorid")
    )
    private Set<Director> directors;

    @ManyToMany
    @JoinTable(
            name = "filmactors",
            joinColumns = @JoinColumn(name = "filmid"),
            inverseJoinColumns = @JoinColumn(name = "actorid")
    )
    private Set<Actor> actors;

    @ManyToMany
    @JoinTable(
            name = "filmtags",
            joinColumns = @JoinColumn(name = "filmid"),
            inverseJoinColumns = @JoinColumn(name = "tagid")
    )
    private Set<Tag> tags;
}
