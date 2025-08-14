// File: src/main/java/com/example/kino/tag/TagPreferenceRepository.java
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
    @Query("UPDATE TagPreference tp SET tp.affinityscore = tp.affinityscore + :amount " +
           "WHERE tp.user = :user AND tp.tag.id = :tagId")
    void increment(@Param("user") User user, @Param("tagId") Integer tagId, @Param("amount") int amount);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO tag_preference(user_id, tag_id, affinityscore) " +
                   "VALUES (:userId, :tagId, :amount) " +
                   "ON CONFLICT (user_id, tag_id) DO UPDATE " +
                   "SET affinityscore = tag_preference.affinityscore + EXCLUDED.affinityscore", nativeQuery = true)
    void incrementOrInsert(@Param("userId") Integer userId, @Param("tagId") Integer tagId, @Param("amount") int amount);
}
