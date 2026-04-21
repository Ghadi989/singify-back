package com.gray.singifyback.dto.response;

public record SongResponse(
    String id,
    String title,
    String artist,
    String coverUrl,
    String audioUrl,
    String duration,
    boolean userLike,
    String previewUrl   // Spotify 30s preview; null for local DB songs
) {}
