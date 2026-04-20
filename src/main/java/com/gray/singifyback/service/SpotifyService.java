package com.gray.singifyback.service;

import com.gray.singifyback.dto.response.SongResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";

    private final RestTemplate restTemplate;
    private final YtDlpService ytDlpService;
    private final String clientId;
    private final String clientSecret;

    // Simple in-memory token cache
    private String cachedToken;
    private long tokenExpiresAt = 0;

    public SpotifyService(RestTemplate restTemplate,
                          YtDlpService ytDlpService,
                          @Value("${spotify.client-id}") String clientId,
                          @Value("${spotify.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
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
            String url = SEARCH_URL + "?q=" + query + "&type=track&limit=20";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null) return Collections.emptyList();

            Map<?, ?> tracks = (Map<?, ?>) body.get("tracks");
            List<?> items = (List<?>) tracks.get("items");

            return items.stream()
                    .map(item -> mapTrack((Map<?, ?>) item))
                    .toList();

        } catch (Exception e) {
            log.error("Spotify search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

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

        int durationMs = (int) track.get("duration_ms");
        String duration = formatDuration(durationMs);

        String audioUrl = ytDlpService.getProxyStreamUrl(artist, title);

        return new SongResponse("spotify-" + id, title, artist, coverUrl, audioUrl, duration, false);
    }

    private String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<Map> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);

        Map<?, ?> responseBody = response.getBody();
        cachedToken = (String) responseBody.get("access_token");
        int expiresIn = (int) responseBody.get("expires_in");
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
