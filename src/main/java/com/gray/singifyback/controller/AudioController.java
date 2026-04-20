package com.gray.singifyback.controller;

import com.gray.singifyback.service.YtDlpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private static final Logger log = LoggerFactory.getLogger(AudioController.class);

    private final YtDlpService ytDlpService;
    private final RestTemplate restTemplate;

    public AudioController(YtDlpService ytDlpService, RestTemplate restTemplate) {
        this.ytDlpService = ytDlpService;
        this.restTemplate = restTemplate;
    }

    /**
     * Proxies audio stream for a given artist + title through Spring Boot.
     * Frontend uses this URL so it never needs to reach the yt-dlp container directly.
     */
    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(
            @RequestParam String artist,
            @RequestParam String title) {

        log.info("Audio stream request: {} - {}", artist, title);

        // Resolve the raw YouTube URL via yt-dlp (benefits from server-side caching)
        String rawAudioUrl = ytDlpService.resolveAudioUrl(artist, title);

        StreamingResponseBody body = outputStream -> {
            try {
                URI uri = URI.create(rawAudioUrl);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(120_000);
                // Forward range header if present (seeking support)
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
