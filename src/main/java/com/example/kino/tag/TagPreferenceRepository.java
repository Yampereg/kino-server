package com.example.kino.tag;

import com.example.kino.user.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TagPreferenceRepository extends JpaRepository<TagPreference, TagPreferenceKey> {

    Optional<TagPreference> findByUserAndTag(User user, Tag tag);
    List<TagPreference> findByUser(User user);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tag_preference(user_id, tag_id, affinityscore)
        VALUES (:userId, :tagId, :amount)
        ON CONFLICT (user_id, tag_id)
        DO UPDATE SET affinityscore = tag_preference.affinityscore + EXCLUDED.affinityscore
    """, nativeQuery = true)
    void incrementOrInsert(@Param("userId") Integer userId,
                           @Param("tagId") Integer tagId,
                           @Param("amount") int amount);
}
