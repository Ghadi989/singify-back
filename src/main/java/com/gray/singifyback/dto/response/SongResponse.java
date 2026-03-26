package com.gray.singifyback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongResponse {
    private String id;
    private String title;
    private String artist;
    private String coverUrl;
    private String audioUrl;
    private String duration;
    private boolean userLike;
}
