package com.gray.singifyback.controller;

import com.gray.singifyback.service.AudioProcessingService;
import com.gray.singifyback.service.GcsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Simple admin endpoints for inspecting and managing GCS karaoke cache.
 * Accessible at /api/admin/gcs/**
 */
@RestController
@RequestMapping("/api/admin/gcs")
public class AdminController {

    private final GcsService gcsService;
    private final AudioProcessingService processingService;

    public AdminController(GcsService gcsService, AudioProcessingService processingService) {
        this.gcsService = gcsService;
        this.processingService = processingService;
    }

    /** List all cached audio files with size and last-updated timestamp. */
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        return ResponseEntity.ok(gcsService.listFiles());
    }

    /** Delete a specific file by its GCS key (e.g. audio/adele__someone_like_you.mp3) */
    @DeleteMapping("/files")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam String key) {
        boolean deleted = gcsService.delete(key);
        return ResponseEntity.ok(Map.of("deleted", deleted, "key", key));
    }

    /** Delete by artist + title (same normalisation used when uploading). */
    @DeleteMapping("/files/by-song")
    public ResponseEntity<Map<String, Object>> deleteBySong(
            @RequestParam String artist,
            @RequestParam String title) {
        String key = GcsService.toKey(artist, title);
        boolean deleted = gcsService.delete(key);
        return ResponseEntity.ok(Map.of("deleted", deleted, "key", key, "artist", artist, "title", title));
    }

    /** Get a fresh signed URL for any cached file (valid 6 h). */
    @GetMapping("/files/url")
    public ResponseEntity<Map<String, Object>> getUrl(@RequestParam String key) {
        if (!gcsService.exists(key)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("key", key, "url", gcsService.getSignedUrl(key)));
    }

    /** Check in-memory processing status (not persisted across restarts). */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> processingStatus(
            @RequestParam String artist,
            @RequestParam String title) {
        String status = processingService.getStatus(artist, title);
        return ResponseEntity.ok(Map.of("status", status, "artist", artist, "title", title));
    }
}
