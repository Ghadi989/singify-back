package com.gray.singifyback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class AudioProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AudioProcessingService.class);

    private final YtDlpService ytDlpService;
    private final GcsService gcsService;

    // key → "processing" | "ready" | "failed"
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();

    public AudioProcessingService(YtDlpService ytDlpService, GcsService gcsService) {
        this.ytDlpService = ytDlpService;
        this.gcsService = gcsService;
    }

    public String getStatus(String artist, String title) {
        String key = GcsService.toKey(artist, title);
        if (gcsService.isEnabled() && gcsService.exists(key)) return "ready";
        return statusMap.getOrDefault(key, "not_started");
    }

    public String getSignedUrl(String artist, String title) {
        return gcsService.getSignedUrl(GcsService.toKey(artist, title));
    }

    @Async("karaokeExecutor")
    public void processAsync(String spotifyId, String artist, String title) {
        String key = GcsService.toKey(artist, title);

        // Guard: don't double-process
        if ("processing".equals(statusMap.get(key))) return;
        statusMap.put(key, "processing");

        if (!gcsService.isEnabled()) {
            statusMap.put(key, "failed");
            log.error("❌ [BACKEND] GCS is not configured — cannot store karaoke. " +
                      "Check gcs.bucket-name and gcs.credentials-path in application.yml");
            return;
        }

        try {
            log.info("▶ [BACKEND] Requesting instrumental from yt-dlp service: {} - {}", artist, title);
            byte[] mp3 = ytDlpService.processKaraoke(spotifyId, artist, title);
            log.info("▶ [BACKEND] Received {} KB — uploading to GCS: {}", mp3.length / 1024, key);
            gcsService.upload(key, mp3, "audio/mpeg");
            statusMap.put(key, "ready");
            log.info("✅ [BACKEND] GCS upload complete — karaoke ready: {}", key);
        } catch (Exception e) {
            statusMap.put(key, "failed");
            log.error("❌ [BACKEND] Pipeline failed for {} - {}: {}", artist, title, e.getMessage());
        }
    }
}
