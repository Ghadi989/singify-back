package com.gray.singifyback.dto.request;

public record LikeRequest(
    String title,
    String artist,
    String coverUrl,
    String duration,
    String spotifyId,
    String previewUrl
) {}
