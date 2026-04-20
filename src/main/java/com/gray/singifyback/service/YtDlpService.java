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
    private final String baseUrl;

    public YtDlpService(RestTemplate restTemplate,
                        @Value("${ytdlp.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns a streaming URL for the given song from the yt-dlp microservice.
     * Falls back to null if the service is unavailable.
     */
    public String getStreamUrl(String artist, String title) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/url")
                    .queryParam("artist", artist)
                    .queryParam("title", title)
                    .toUriString();
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("url") instanceof String streamUrl) {
                return streamUrl;
            }
        } catch (Exception e) {
            log.warn("yt-dlp service unavailable for '{} - {}': {}", artist, title, e.getMessage());
        }
        return null;
    }

    /**
     * Returns the proxy stream URL (served through our ytdlp service, handles CORS).
     */
    public String getProxyStreamUrl(String artist, String title) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl + "/stream")
                .queryParam("artist", artist)
                .queryParam("title", title)
                .toUriString();
    }
}
