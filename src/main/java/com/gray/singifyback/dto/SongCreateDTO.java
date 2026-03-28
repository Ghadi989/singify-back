package com.gray.singifyback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongCreateDTO {

    private String spotifyId;

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotBlank(message = "Artist must not be blank")
    private String artist;

    private String genre;

    @NotNull(message = "Release year must not be null")
    private Integer releaseYear;

    private String albumCoverUrl;

    @NotNull(message = "Duration must not be null")
    private Long durationMs;
}
