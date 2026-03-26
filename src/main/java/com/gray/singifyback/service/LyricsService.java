package com.gray.singifyback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class LyricsService {

    @Value("${lrclib.base-url}")
    private String lrclibBaseUrl;

    private final RestTemplate restTemplate;

    // GET /get?artist_name={artist}&track_name={title}
    public Object getLyrics(String title, String artist) {
        String url = UriComponentsBuilder.fromHttpUrl(lrclibBaseUrl + "/get")
                .queryParam("artist_name", artist)
                .queryParam("track_name", title)
                .toUriString();
        return restTemplate.getForObject(url, Object.class);
    }
}
