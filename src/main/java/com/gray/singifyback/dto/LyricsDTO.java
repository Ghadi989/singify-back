package com.gray.singifyback.dto;

import java.util.List;

public record LyricsDTO(Long songId, List<LyricLine> lines, String source) {

    public record LyricLine(String timestamp, String line) {}
}
