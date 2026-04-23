package com.gray.singifyback.unit;

import com.gray.singifyback.service.YtDlpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YtDlpServiceTest {

    @Mock RestTemplate restTemplate;

    YtDlpService ytDlpService;

    @BeforeEach
    void setUp() {
        ytDlpService = new YtDlpService(restTemplate, "http://ytdlp:8000", "http://localhost:8080");
    }

    @Test
    void getProxyStreamUrl_returnsBackendStreamEndpoint() {
        String url = ytDlpService.getProxyStreamUrl("Adele", "Hello");

        assertThat(url).startsWith("http://localhost:8080/api/audio/stream");
        assertThat(url).contains("artist=Adele");
        assertThat(url).contains("title=Hello");
    }

    @Test
    void getInternalStreamUrl_returnsYtDlpStreamEndpoint() {
        String url = ytDlpService.getInternalStreamUrl("Adele", "Hello");

        assertThat(url).startsWith("http://ytdlp:8000/stream");
        assertThat(url).contains("artist=Adele");
        assertThat(url).contains("title=Hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveAudioUrl_returnsUrlFromYtDlp() {
        when(restTemplate.getForObject(any(java.net.URI.class), eq(Map.class)))
                .thenReturn(Map.of("url", "https://audio.youtube.com/stream/abc123"));

        String url = ytDlpService.resolveAudioUrl("Adele", "Hello");

        assertThat(url).isEqualTo("https://audio.youtube.com/stream/abc123");
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveAudioUrl_nullResponse_throwsException() {
        when(restTemplate.getForObject(any(java.net.URI.class), eq(Map.class))).thenReturn(null);

        assertThatThrownBy(() -> ytDlpService.resolveAudioUrl("Adele", "Hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no URL");
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolveAudioUrl_emptyMap_throwsException() {
        when(restTemplate.getForObject(any(java.net.URI.class), eq(Map.class))).thenReturn(Map.of());

        assertThatThrownBy(() -> ytDlpService.resolveAudioUrl("Adele", "Hello"))
                .isInstanceOf(RuntimeException.class);
    }
}
