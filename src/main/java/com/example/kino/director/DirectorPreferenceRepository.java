// File: src/main/java/com/example/kino/director/DirectorPreferenceRepository.java
package com.example.kino.director;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DirectorPreferenceRepository extends JpaRepository<DirectorPreference, DirectorPreferenceKey> {

    Optional<DirectorPreference> findByUserAndDirector(User user, Director director);
    List<DirectorPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO director_preference (user_id, director_id, affinityscore)
            VALUES (:userId, :directorId, :amount)
            ON CONFLICT (user_id, director_id)
            DO UPDATE SET affinityscore = director_preference.affinityscore + EXCLUDED.affinityscore
            """, nativeQuery = true)
    void upsertIncrement(@Param("userId") Integer userId,
                         @Param("directorId") Integer directorId,
                         @Param("amount") int amount);
}
