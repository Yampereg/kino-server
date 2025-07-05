package com.example.kino.director;
import java.util.List;
import java.util.Optional;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DirectorPreferenceRepository extends JpaRepository<DirectorPreference, DirectorPreferenceKey> {

    Optional<DirectorPreference> findByUserAndDirector(User user, Director director);
    List<DirectorPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query("UPDATE DirectorPreference dp SET dp.affinityscore = dp.affinityscore + :amount WHERE dp.user = :user AND dp.director.id = :directorId")
    void increment(@Param("user") User user, @Param("directorId") Integer directorId, @Param("amount") int amount);
}
