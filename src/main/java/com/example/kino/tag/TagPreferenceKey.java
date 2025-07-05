package com.example.kino.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagPreferenceKey implements Serializable {

    @Column(name = "user_id")  
    private Integer userId;

    @Column(name = "tag_id")   
    private Integer tagId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagPreferenceKey)) return false;
        TagPreferenceKey that = (TagPreferenceKey) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tagId);
    }
}
