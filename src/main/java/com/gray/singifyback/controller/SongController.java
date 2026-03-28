package com.gray.singifyback.controller;

import com.gray.singifyback.dto.SongCreateDTO;
import com.gray.singifyback.dto.SongDTO;
import com.gray.singifyback.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping
    public ResponseEntity<List<SongDTO>> getAllSongs() {
        return ResponseEntity.ok(songService.getAllSongs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDTO> getSongById(@PathVariable Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongDTO>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artist) {
        if (title != null) {
            return ResponseEntity.ok(songService.searchByTitle(title));
        }
        if (artist != null) {
            return ResponseEntity.ok(songService.searchByArtist(artist));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SongDTO>> filter(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year) {
        if (genre != null) {
            return ResponseEntity.ok(songService.filterByGenre(genre));
        }
        if (year != null) {
            return ResponseEntity.ok(songService.filterByYear(year));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping
    public ResponseEntity<SongDTO> createSong(@Valid @RequestBody SongCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(songService.createSong(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SongDTO> updateSong(@PathVariable Long id,
                                              @Valid @RequestBody SongCreateDTO dto) {
        return ResponseEntity.ok(songService.updateSong(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return ResponseEntity.noContent().build();
    }
}
