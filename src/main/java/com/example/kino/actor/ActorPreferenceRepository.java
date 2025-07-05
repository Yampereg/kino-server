package com.example.kino.actor;
import java.util.List;
import java.util.Optional;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ActorPreferenceRepository extends JpaRepository<ActorPreference, ActorPreferenceKey> {

    Optional<ActorPreference> findByUserAndActor(User user, Actor actor);
    List<ActorPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query("UPDATE ActorPreference ap SET ap.affinityscore = ap.affinityscore + :amount WHERE ap.user = :user AND ap.actor.id = :actorId")
    void increment(@Param("user") User user, @Param("actorId") Integer actorId, @Param("amount") int amount);
}
