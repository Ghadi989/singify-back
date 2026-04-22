package com.gray.singifyback.controller;

import com.gray.singifyback.dto.request.LikeRequest;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public ResponseEntity<List<SongResponse>> getLikedSongs(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(libraryService.getLikedSongs(userDetails.getUsername()));
    }

    @PostMapping("/{songId}")
    public ResponseEntity<Map<String, String>> likeSong(
            @PathVariable String songId,
            @RequestBody(required = false) LikeRequest metadata,
            @AuthenticationPrincipal UserDetails userDetails) {
        String realId = libraryService.likeSong(userDetails.getUsername(), songId, metadata);
        return ResponseEntity.ok(Map.of("id", realId));
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> unlikeSong(@PathVariable String songId,
            @AuthenticationPrincipal UserDetails userDetails) {
        libraryService.unlikeSong(userDetails.getUsername(), songId);
        return ResponseEntity.ok().build();
    }
}
