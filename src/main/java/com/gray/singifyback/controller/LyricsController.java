package com.gray.singifyback.controller;

import com.gray.singifyback.dto.LyricsDTO;
import com.gray.singifyback.service.LyricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lyrics")
@RequiredArgsConstructor
public class LyricsController {

    private final LyricsService lyricsService;

    @GetMapping("/{songId}")
    public ResponseEntity<LyricsDTO> getLyrics(@PathVariable Long songId) {
        return ResponseEntity.ok(lyricsService.getLyricsBySongId(songId));
    }

    @PostMapping
    public ResponseEntity<LyricsDTO> saveLyrics(@RequestBody LyricsDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lyricsService.saveLyrics(dto));
    }
}
