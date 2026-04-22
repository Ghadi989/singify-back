package com.gray.singifyback.service;

import com.gray.singifyback.model.Song;
import com.gray.singifyback.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AudioProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AudioProcessingService.class);

    private final YtDlpService ytDlpService;
    private final GcsService gcsService;
    private final SongRepository songRepository;

    // key → "processing" | "ready" | "failed"
    private final Map<String, String> statusMap = new ConcurrentHashMap<>();

    public AudioProcessingService(YtDlpService ytDlpService,
                                  GcsService gcsService,
                                  SongRepository songRepository) {
        this.ytDlpService = ytDlpService;
        this.gcsService = gcsService;
        this.songRepository = songRepository;
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
    public void processAsync(String spotifyId, String artist, String title,
                             String coverUrl, String duration, String previewUrl) {
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

            // Persist song metadata to DB so it appears in the home screen after restart
            saveSongToDb(spotifyId, artist, title, coverUrl, duration, previewUrl);

        } catch (Exception e) {
            statusMap.put(key, "failed");
            log.error("❌ [BACKEND] Pipeline failed for {} - {}: {}", artist, title, e.getMessage());
        }
    }

    private void saveSongToDb(String spotifyId, String artist, String title,
                               String coverUrl, String duration, String previewUrl) {
        try {
            if (songRepository.findBySpotifyId(spotifyId).isPresent()) {
                log.info("ℹ [BACKEND] Song already in DB: {} - {}", artist, title);
                return;
            }
            Song song = new Song();
            song.setSpotifyId(spotifyId);
            song.setTitle(title);
            song.setArtist(artist);
            song.setCoverUrl(coverUrl);
            song.setDuration(duration);
            song.setPreviewUrl(previewUrl);
            song.setAudioUrl(null); // resolved at request time via yt-dlp proxy
            songRepository.save(song);
            log.info("✅ [BACKEND] Song saved to DB: {} - {}", artist, title);
        } catch (Exception e) {
            // Non-fatal — karaoke is still in GCS, song just won't appear in recent list on next load
            log.warn("⚠ [BACKEND] Could not save song to DB ({} - {}): {}", artist, title, e.getMessage());
        }
    }
}
