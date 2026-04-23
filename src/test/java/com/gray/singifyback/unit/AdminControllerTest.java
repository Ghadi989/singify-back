package com.gray.singifyback.unit;

import com.gray.singifyback.controller.AdminController;
import com.gray.singifyback.service.AudioProcessingService;
import com.gray.singifyback.service.GcsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock GcsService gcsService;
    @Mock AudioProcessingService processingService;

    @InjectMocks AdminController adminController;

    @Test
    void listFiles_returnsGcsFileList() {
        List<Map<String, Object>> files = List.of(
                Map.of("key", "audio/adele-hello.mp3", "size", 1024L)
        );
        when(gcsService.listFiles()).thenReturn(files);

        ResponseEntity<List<Map<String, Object>>> resp = adminController.listFiles();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo(files);
    }

    @Test
    void deleteFile_delegatesToGcsAndReturnsResult() {
        when(gcsService.delete("audio/adele-hello.mp3")).thenReturn(true);

        ResponseEntity<Map<String, Object>> resp = adminController.deleteFile("audio/adele-hello.mp3");

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("deleted", true);
        assertThat(resp.getBody()).containsEntry("key", "audio/adele-hello.mp3");
    }

    @Test
    void deleteBySong_computesKeyAndDeletes() {
        String expectedKey = GcsService.toKey("Adele", "Hello");
        when(gcsService.delete(expectedKey)).thenReturn(true);

        ResponseEntity<Map<String, Object>> resp = adminController.deleteBySong("Adele", "Hello");

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("artist", "Adele");
        assertThat(resp.getBody()).containsEntry("title", "Hello");
        verify(gcsService).delete(expectedKey);
    }

    @Test
    void getUrl_whenFileExists_returnsSignedUrl() {
        String key = "audio/adele-hello.mp3";
        when(gcsService.exists(key)).thenReturn(true);
        when(gcsService.getSignedUrl(key)).thenReturn("https://signed.url/audio");

        ResponseEntity<Map<String, Object>> resp = adminController.getUrl(key);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("url", "https://signed.url/audio");
    }

    @Test
    void getUrl_whenFileNotFound_returns404() {
        when(gcsService.exists("missing.mp3")).thenReturn(false);

        ResponseEntity<Map<String, Object>> resp = adminController.getUrl("missing.mp3");

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void processingStatus_returnsStatusFromService() {
        when(processingService.getStatus("Adele", "Hello")).thenReturn("ready");

        ResponseEntity<Map<String, String>> resp = adminController.processingStatus("Adele", "Hello");

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).containsEntry("status", "ready");
        assertThat(resp.getBody()).containsEntry("artist", "Adele");
    }
}
