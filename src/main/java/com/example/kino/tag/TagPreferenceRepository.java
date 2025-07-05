package com.example.kino.tag;
import java.util.List;
import java.util.Optional;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TagPreferenceRepository extends JpaRepository<TagPreference, TagPreferenceKey> {

    Optional<TagPreference> findByUserAndTag(User user, Tag tag);
    List<TagPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query("UPDATE TagPreference tp SET tp.affinityscore = tp.affinityscore + :amount WHERE tp.user = :user AND tp.tag.id = :tagId")
    void increment(@Param("user") User user, @Param("tagId") Integer tagId, @Param("amount") int amount);
}
