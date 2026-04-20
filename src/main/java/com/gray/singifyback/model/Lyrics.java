package com.gray.singifyback.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lyrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lyrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long songId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lines;

    private String source;
}
