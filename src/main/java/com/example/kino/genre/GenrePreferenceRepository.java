package com.example.kino.genre;
import java.util.List;
import java.util.Optional;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GenrePreferenceRepository extends JpaRepository<GenrePreference, GenrePreferenceKey> {

    Optional<GenrePreference> findByUserAndGenre(User user, Genre tag);
    List<GenrePreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query("UPDATE GenrePreference gp SET gp.affinityscore = gp.affinityscore + :amount WHERE gp.user = :user AND gp.genre.id = :genreId")
    void increment(@Param("user") User user, @Param("genreId") Integer genreId, @Param("amount") int amount);
}
