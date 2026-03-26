package com.gray.singifyback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LyricsService {

    @Value("${lrclib.base-url}")
    private String lrclibBaseUrl;

    private final RestTemplate restTemplate;

    public LyricsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getLyrics(String title, String artist) {
        // lrclib requires + for spaces, not %20
        String url = lrclibBaseUrl + "/get?artist_name=" +
                artist.replace(" ", "+") + "&track_name=" + title.replace(" ", "+");

        Map<String, Object> raw;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            raw = result;
        } catch (RestClientException e) {
            // lrclib returned 404 or other error — return empty lyrics gracefully
            return Map.of("lyrics", List.of());
        }

        if (raw == null) {
            return Map.of("lyrics", List.of());
        }

        String syncedLyrics = (String) raw.get("syncedLyrics");
        if (syncedLyrics == null || syncedLyrics.isBlank()) {
            return Map.of("lyrics", List.of());
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        for (String line : syncedLyrics.split("\n")) {
            // format: [MM:SS.xx] text
            if (!line.startsWith("[")) continue;
            int closingBracket = line.indexOf(']');
            if (closingBracket == -1) continue;

            String timestamp = line.substring(1, closingBracket); // "00:16.86"
            String text = line.substring(closingBracket + 1).trim();

            lines.add(Map.of(
                "timestamp", timestamp,
                "line",      text,
                "seconds",   toSeconds(timestamp)
            ));
        }

        return Map.of("lyrics", lines);
    }

    // "01:16.86" → 76.86
    private double toSeconds(String timestamp) {
        try {
            String[] parts = timestamp.split(":");
            double minutes = Double.parseDouble(parts[0]);
            double seconds = Double.parseDouble(parts[1]);
            return minutes * 60 + seconds;
        } catch (Exception e) {
            return 0;
        }
    }
}
