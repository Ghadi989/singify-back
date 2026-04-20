package com.gray.singifyback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongDTO {

    private Long id;
    private String spotifyId;
    private String title;
    private String artist;
    private String genre;
    private Integer releaseYear;
    private String albumCoverUrl;
    private Long durationMs;
}
