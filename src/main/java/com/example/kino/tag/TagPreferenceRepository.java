// File: src/main/java/com/example/kino/tag/TagPreferenceRepository.java
package com.example.kino.tag;

import com.example.kino.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagPreferenceRepository extends JpaRepository<TagPreference, TagPreferenceKey> {

    Optional<TagPreference> findByUserAndTag(User user, Tag tag);
    List<TagPreference> findByUser(User user);

    // Native UPSERT – creates the row if missing, otherwise increments.
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO tag_preference (user_id, tag_id, affinityscore)
            VALUES (:userId, :tagId, :amount)
            ON CONFLICT (user_id, tag_id)
            DO UPDATE SET affinityscore = tag_preference.affinityscore + EXCLUDED.affinityscore
            """, nativeQuery = true)
    void upsertIncrement(@Param("userId") Integer userId,
                         @Param("tagId") Integer tagId,
                         @Param("amount") int amount);
}
