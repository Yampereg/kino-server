package com.example.kino.userinteraction;

import com.example.kino.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFilmInteractionRepository extends JpaRepository<UserFilmInteraction, UserFilmInteractionKey> {
    List<UserFilmInteraction> findAllByUser(User user);
}
