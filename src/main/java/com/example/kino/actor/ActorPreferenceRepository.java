// File: src/main/java/com/example/kino/actor/ActorPreferenceRepository.java
package com.example.kino.actor;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActorPreferenceRepository extends JpaRepository<ActorPreference, ActorPreferenceKey> {

    Optional<ActorPreference> findByUserAndActor(User user, Actor actor);
    List<ActorPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO actor_preference (user_id, actor_id, affinityscore)
            VALUES (:userId, :actorId, :amount)
            ON CONFLICT (user_id, actor_id)
            DO UPDATE SET affinityscore = actor_preference.affinityscore + EXCLUDED.affinityscore
            """, nativeQuery = true)
    void upsertIncrement(@Param("userId") Integer userId,
                         @Param("actorId") Integer actorId,
                         @Param("amount") int amount);
}
