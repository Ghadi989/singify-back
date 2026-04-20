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
