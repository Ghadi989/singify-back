package com.gray.singifyback.controller;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Void> likeSong(@PathVariable String songId,
            @AuthenticationPrincipal UserDetails userDetails) {
        libraryService.likeSong(userDetails.getUsername(), songId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> unlikeSong(@PathVariable String songId,
            @AuthenticationPrincipal UserDetails userDetails) {
        libraryService.unlikeSong(userDetails.getUsername(), songId);
        return ResponseEntity.ok().build();
    }
}
