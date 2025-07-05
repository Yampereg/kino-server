package com.example.kino.tag;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @Column(name = "tagid")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tags_tagid_seq")
    @SequenceGenerator(name = "tags_tagid_seq", sequenceName = "tags_tagid_seq", allocationSize = 1)
    private Integer id;

    @Column(unique = true, length = 255)
    private String name;
}
