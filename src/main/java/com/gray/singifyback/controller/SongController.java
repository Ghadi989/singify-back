package com.gray.singifyback.controller;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.service.LyricsService;
import com.gray.singifyback.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final LyricsService lyricsService;

    @GetMapping
    public ResponseEntity<List<SongResponse>> getAllSongs(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(songService.getAllSongs(email));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongResponse>> searchSongs(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(songService.searchSongs(q, email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getSongById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(songService.getSongById(id, email));
    }

    @GetMapping("/{id}/lyrics")
    public ResponseEntity<Object> getLyrics(@PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        SongResponse song = songService.getSongById(id,
                userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(lyricsService.getLyrics(song.getTitle(), song.getArtist()));
    }
}
