package com.gray.singifyback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gray.singifyback.dto.response.SongResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final YtDlpService ytDlpService;
    private final String clientId;
    private final String clientSecret;

    // Simple in-memory token cache
    private String cachedToken;
    private long tokenExpiresAt = 0;

    public SpotifyService(YtDlpService ytDlpService,
                          @Value("${spotify.client-id}") String clientId,
                          @Value("${spotify.client-secret}") String clientSecret) {
        this.ytDlpService = ytDlpService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    public List<SongResponse> search(String query) {
        if (!isConfigured()) {
            log.warn("Spotify not configured — skipping search");
            return Collections.emptyList();
        }
        try {
            String token = getAccessToken();
            String url = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&type=track&market=FR";
            log.info("Spotify search URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Spotify search status: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("Spotify search error {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            Map<?, ?> body = mapper.readValue(response.body(), Map.class);
            Map<?, ?> tracks = (Map<?, ?>) body.get("tracks");
            if (tracks == null) { log.error("No 'tracks' in response: {}", response.body()); return Collections.emptyList(); }
            List<?> items = (List<?>) tracks.get("items");
            if (items == null) return Collections.emptyList();

            return items.stream()
                    .map(item -> mapTrack((Map<?, ?>) item))
                    .toList();

        } catch (Exception e) {
            log.error("Spotify search failed for '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private SongResponse mapTrack(Map<?, ?> track) {
        String id = (String) track.get("id");
        String title = (String) track.get("name");

        List<?> artists = (List<?>) track.get("artists");
        String artist = artists.isEmpty() ? "Unknown"
                : (String) ((Map<?, ?>) artists.get(0)).get("name");

        Map<?, ?> album = (Map<?, ?>) track.get("album");
        List<?> images = (List<?>) album.get("images");
        String coverUrl = images.isEmpty() ? null
                : (String) ((Map<?, ?>) images.get(0)).get("url");

        Number durationMs = (Number) track.get("duration_ms");
        String duration = formatDuration(durationMs != null ? durationMs.intValue() : 0);

        String previewUrl = (String) track.get("preview_url");
        String audioUrl = ytDlpService.getProxyStreamUrl(artist, title);

        return new SongResponse("spotify-" + id, title, artist, coverUrl, audioUrl, duration, false, previewUrl);
    }

    private String getAccessToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }
        log.info("Requesting Spotify token for clientId={}", clientId);

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String formBody = "grant_type=client_credentials";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token request failed " + response.statusCode() + ": " + response.body());
        }

        Map<?, ?> body = mapper.readValue(response.body(), Map.class);
        cachedToken = (String) body.get("access_token");
        int expiresIn = ((Number) body.get("expires_in")).intValue();
        tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

        return cachedToken;
    }

    private String formatDuration(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
