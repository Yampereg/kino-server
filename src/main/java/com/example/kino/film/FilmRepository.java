package com.example.kino.film;

import com.example.kino.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilmRepository extends JpaRepository<Film, Integer> {

    @Query("SELECT f FROM Film f WHERE f.id NOT IN (SELECT i.film.id FROM com.example.kino.userinteraction.UserFilmInteraction i WHERE i.user = :user)")
    List<Film> findUnseenFilms(@Param("user") User user);
    
    @Query("SELECT f FROM Film f WHERE f.id NOT IN (SELECT i.film.id FROM com.example.kino.userinteraction.UserFilmInteraction i WHERE i.user = :user)")
    Page<Film> findUnseenFilmsPage(@Param("user") User user, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT f FROM Film f WHERE f.id NOT IN (SELECT i.film.id FROM com.example.kino.userinteraction.UserFilmInteraction i WHERE i.user = :user) ORDER BY f.popularity DESC")
    List<Film> findTopUnseenByUser(@Param("user") User user, org.springframework.data.domain.Pageable pageable);

}
