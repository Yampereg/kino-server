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
    int incrementRows(@Param("user") User user, @Param("tagId") Integer tagId, @Param("amount") int amount);

    @Transactional
    default void increment(User user, Tag tag, int amount) {
        // Try to increment first
        int updated = incrementRows(user, tag.getId(), amount);
        if (updated == 0) {
            // No existing row → insert new
            save(TagPreference.builder()
                    .id(new TagPreferenceKey(user.getId(), tag.getId()))
                    .user(user)
                    .tag(tag)
                    .affinityscore(amount)
                    .build());
        }
    }
}
