package com.gray.singifyback.dto.response;

public record SongResponse(
    String id,
    String title,
    String artist,
    String coverUrl,
    String audioUrl,
    String duration,
    boolean userLike
) {}
