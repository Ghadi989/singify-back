package com.gray.singifyback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class YtDlpService {

    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);

    private final RestTemplate restTemplate;
    private final String ytdlpBaseUrl;
    private final String backendBaseUrl;

    public YtDlpService(RestTemplate restTemplate,
                        @Value("${ytdlp.base-url}") String ytdlpBaseUrl,
                        @Value("${backend.base-url:http://localhost:8080}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.ytdlpBaseUrl = ytdlpBaseUrl;
        this.backendBaseUrl = backendBaseUrl;
        log.info("YtDlpService init — ytdlpBaseUrl='{}' envVar='{}'",
                ytdlpBaseUrl, System.getenv("YTDLP_URL"));
    }

    /**
     * Returns a URL the browser can use to stream audio.
     * Points to the Spring Boot proxy endpoint (/api/audio/stream),
     * which internally calls the yt-dlp container.
     */
    public String getProxyStreamUrl(String artist, String title) {
        return UriComponentsBuilder.fromHttpUrl(backendBaseUrl + "/api/audio/stream")
                .queryParam("artist", artist)
                .queryParam("title", title)
                .toUriString();
    }

    /**
     * Calls yt-dlp /url endpoint to get the resolved raw audio URL (server-to-server).
     * The yt-dlp service caches results so repeated calls are fast.
     */
    @SuppressWarnings("unchecked")
    public String resolveAudioUrl(String artist, String title) {
        // Use toUri() so RestTemplate receives a java.net.URI and does NOT double-encode.
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(ytdlpBaseUrl + "/url")
                .queryParam("artist", artist)
                .queryParam("title", title)
                .build().toUri();
        Map<String, String> response = restTemplate.getForObject(uri, Map.class);
        if (response == null || !response.containsKey("url")) {
            throw new RuntimeException("yt-dlp returned no URL for " + artist + " - " + title);
        }
        return response.get("url");
    }

    /**
     * Calls yt-dlp /process: spotDL download + Spleeter vocal removal → instrumental MP3 bytes.
     */
    public byte[] processKaraoke(String spotifyId, String artist, String title) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(ytdlpBaseUrl + "/process")
                .queryParam("spotify_id", spotifyId)
                .queryParam("artist", artist)
                .queryParam("title", title)
                .build().toUri();
        log.info("Requesting karaoke processing: {} - {} ({})", artist, title, spotifyId);
        return restTemplate.getForObject(uri, byte[].class);
    }

    /**
     * Calls yt-dlp /download endpoint to get the full MP3 bytes.
     * yt-dlp handles YouTube throttle bypass internally during download.
     */
    public byte[] downloadMp3(String artist, String title) {
        java.net.URI uri = UriComponentsBuilder.fromHttpUrl(ytdlpBaseUrl + "/download")
                .queryParam("artist", artist)
                .queryParam("title", title)
                .build().toUri();
        log.info("Downloading MP3 from yt-dlp: {} - {}", artist, title);
        return restTemplate.getForObject(uri, byte[].class);
    }

    /**
     * Calls yt-dlp service directly (server-to-server) to get the raw stream URL.
     * Used internally by AudioController.
     */
    public String getInternalStreamUrl(String artist, String title) {
        return UriComponentsBuilder.fromHttpUrl(ytdlpBaseUrl + "/stream")
                .queryParam("artist", artist)
                .queryParam("title", title)
                .toUriString();
    }
}
