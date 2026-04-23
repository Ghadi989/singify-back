package com.gray.singifyback.unit;

import com.gray.singifyback.service.LyricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LyricsServiceTest {

    @Mock RestTemplate restTemplate;

    @InjectMocks LyricsService lyricsService;

    @BeforeEach
    void injectBaseUrl() {
        ReflectionTestUtils.setField(lyricsService, "lrclibBaseUrl", "http://lrclib.test");
    }

    @Test
    void getLyrics_parsedSyncedLyrics_returnsLines() {
        String synced = "[00:05.00] Hello\n[00:10.50] World";
        Map<String, Object> apiResponse = Map.of("syncedLyrics", synced);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<String, Object> result = lyricsService.getLyrics("Hello", "Adele");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) result.get("lyrics");
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).get("line")).isEqualTo("Hello");
        assertThat(lines.get(0).get("timestamp")).isEqualTo("00:05.00");
        assertThat((double) lines.get(0).get("seconds")).isEqualTo(5.0);
        assertThat(lines.get(1).get("line")).isEqualTo("World");
    }

    @Test
    void getLyrics_nullResponse_returnsEmptyLyrics() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        Map<String, Object> result = lyricsService.getLyrics("Unknown", "Unknown");

        assertThat((List<?>) result.get("lyrics")).isEmpty();
    }

    @Test
    void getLyrics_emptySyncedLyrics_returnsEmptyLyrics() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("syncedLyrics", ""));

        Map<String, Object> result = lyricsService.getLyrics("Title", "Artist");

        assertThat((List<?>) result.get("lyrics")).isEmpty();
    }

    @Test
    void getLyrics_apiError_returnsEmptyLyrics() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        Map<String, Object> result = lyricsService.getLyrics("Title", "Artist");

        assertThat((List<?>) result.get("lyrics")).isEmpty();
    }

    @Test
    void getLyrics_linesWithoutBrackets_areSkipped() {
        String synced = "[00:01.00] Line one\nno bracket line\n[00:02.00] Line two";
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("syncedLyrics", synced));

        Map<String, Object> result = lyricsService.getLyrics("Song", "Singer");

        @SuppressWarnings("unchecked")
        List<?> lines = (List<?>) result.get("lyrics");
        assertThat(lines).hasSize(2);
    }
}
