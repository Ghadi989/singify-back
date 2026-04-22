package com.gray.singifyback.controller;

import com.gray.singifyback.service.AudioProcessingService;
import com.gray.singifyback.service.GcsService;
import com.gray.singifyback.service.YtDlpService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final YtDlpService ytDlpService;
    private final GcsService gcsService;
    private final AudioProcessingService processingService;

    public AudioController(YtDlpService ytDlpService, GcsService gcsService,
                           AudioProcessingService processingService) {
        this.ytDlpService = ytDlpService;
        this.gcsService = gcsService;
        this.processingService = processingService;
    }

    // ── Karaoke status & trigger ────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status(
            @RequestParam String artist,
            @RequestParam String title) {
        String status = processingService.getStatus(artist, title);
        Map<String, String> resp = new HashMap<>();
        resp.put("status", status);
        if ("ready".equals(status)) {
            resp.put("url", processingService.getSignedUrl(artist, title));
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> process(
            @RequestParam String spotifyId,
            @RequestParam String artist,
            @RequestParam String title,
            @RequestParam(required = false) String coverUrl,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String previewUrl) {
        String current = processingService.getStatus(artist, title);
        if (!"processing".equals(current) && !"ready".equals(current)) {
            processingService.processAsync(spotifyId, artist, title, coverUrl, duration, previewUrl);
        }
        return ResponseEntity.ok(Map.of("status", "processing"));
    }

    // ── Audio streaming ─────────────────────────────────────────────────────────
    //
    // This endpoint serves the ORIGINAL (non-instrumental) version of a song for
    // live playback while the karaoke pipeline is running.
    //
    // IMPORTANT: it must NEVER write to GCS. GCS is exclusively written by the
    // karaoke pipeline (AudioProcessingService). Writing the normal version to GCS
    // would make /status return "ready" with the wrong (non-instrumental) file.

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(
            @RequestParam String artist,
            @RequestParam String title,
            HttpServletResponse response) throws Exception {

        log.info("Stream request (normal): {} - {}", artist, title);

        // If the karaoke version is already in GCS, redirect there so the browser
        // gets the instrumental immediately without going through the pipeline again.
        String key = GcsService.toKey(artist, title);
        if (gcsService.isEnabled() && gcsService.exists(key)) {
            log.info("GCS karaoke hit — redirecting: {}", key);
            response.sendRedirect(gcsService.getSignedUrl(key));
            return null;
        }

        // Otherwise proxy-stream the original song from YouTube.
        // Do NOT cache to GCS here — that is the karaoke pipeline's job.
        log.info("Proxy-streaming from YouTube: {} - {}", artist, title);
        String rawAudioUrl;
        try {
            rawAudioUrl = ytDlpService.resolveAudioUrl(artist, title);
        } catch (Exception e) {
            log.error("Could not resolve audio URL for {} - {}: {}", artist, title, e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Audio not available");
            return null;
        }

        StreamingResponseBody body = outputStream -> {
            try {
                URI uri = URI.create(rawAudioUrl);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(120_000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setRequestProperty("Referer", "https://www.youtube.com/");
                conn.setRequestProperty("Origin", "https://www.youtube.com");
                try (InputStream in = conn.getInputStream()) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                log.error("Stream failed for {} - {}: {}", artist, title, e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Accept-Ranges", "bytes")
                .body(body);
    }
}
