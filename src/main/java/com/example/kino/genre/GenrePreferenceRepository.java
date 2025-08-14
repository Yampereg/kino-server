// File: src/main/java/com/example/kino/genre/GenrePreferenceRepository.java
package com.example.kino.genre;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GenrePreferenceRepository extends JpaRepository<GenrePreference, GenrePreferenceKey> {

    Optional<GenrePreference> findByUserAndGenre(User user, Genre genre);
    List<GenrePreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO genre_preference (user_id, genre_id, affinityscore)
            VALUES (:userId, :genreId, :amount)
            ON CONFLICT (user_id, genre_id)
            DO UPDATE SET affinityscore = genre_preference.affinityscore + EXCLUDED.affinityscore
            """, nativeQuery = true)
    void upsertIncrement(@Param("userId") Integer userId,
                         @Param("genreId") Integer genreId,
                         @Param("amount") int amount);
}
