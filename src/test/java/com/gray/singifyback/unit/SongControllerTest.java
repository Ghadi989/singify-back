package com.gray.singifyback.unit;

import com.gray.singifyback.controller.SongController;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.service.LyricsService;
import com.gray.singifyback.service.SongService;
import com.gray.singifyback.service.SpotifyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongControllerTest {

    @Mock SongService songService;
    @Mock LyricsService lyricsService;
    @Mock SpotifyService spotifyService;

    @InjectMocks SongController songController;

    private SongResponse song(String id) {
        return new SongResponse(id, "Hello", "Adele", null, "http://audio", "4:55", false, null);
    }

    @Test
    void getAllSongs_authenticated_passesEmailToService() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("a@b.com");
        when(songService.getAllSongs("a@b.com")).thenReturn(List.of(song("s1")));

        ResponseEntity<List<SongResponse>> resp = songController.getAllSongs(user);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).hasSize(1);
        verify(songService).getAllSongs("a@b.com");
    }

    @Test
    void getAllSongs_anonymous_passesNullToService() {
        when(songService.getAllSongs(null)).thenReturn(List.of());

        ResponseEntity<List<SongResponse>> resp = songController.getAllSongs(null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(songService).getAllSongs(null);
    }

    @Test
    void getRecommendedSongs_delegatesToGetAllSongs() {
        when(songService.getAllSongs(null)).thenReturn(List.of(song("s1")));

        ResponseEntity<List<SongResponse>> resp = songController.getRecommendedSongs(null);

        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void searchSongs_spotifyNotConfigured_usesLocalDb() {
        when(spotifyService.isConfigured()).thenReturn(false);
        when(songService.searchSongs("adele", null)).thenReturn(List.of(song("s1")));

        ResponseEntity<List<SongResponse>> resp = songController.searchSongs("adele", null);

        assertThat(resp.getBody()).hasSize(1);
        verify(songService).searchSongs("adele", null);
        verify(spotifyService, never()).search(any());
    }

    @Test
    void searchSongs_spotifyConfigured_usesSpotify() {
        when(spotifyService.isConfigured()).thenReturn(true);
        when(spotifyService.search("adele")).thenReturn(List.of(song("spotify-1")));

        ResponseEntity<List<SongResponse>> resp = songController.searchSongs("adele", null);

        assertThat(resp.getBody()).hasSize(1);
        verify(spotifyService).search("adele");
        verify(songService, never()).searchSongs(any(), any());
    }

    @Test
    void getSongById_returnsResponse() {
        when(songService.getSongById("s1", null)).thenReturn(song("s1"));

        ResponseEntity<SongResponse> resp = songController.getSongById("s1", null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody().id()).isEqualTo("s1");
    }

    @Test
    void getLyrics_callsLyricsServiceWithSongTitleAndArtist() {
        when(songService.getSongById("s1", null)).thenReturn(song("s1"));
        when(lyricsService.getLyrics("Hello", "Adele")).thenReturn(Map.of("lyrics", List.of()));

        ResponseEntity<Map<String, Object>> resp = songController.getLyrics("s1", null);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(lyricsService).getLyrics("Hello", "Adele");
    }

    @Test
    void getLyricsDirect_callsLyricsService() {
        when(lyricsService.getLyrics("Hello", "Adele")).thenReturn(Map.of("lyrics", List.of()));

        ResponseEntity<Map<String, Object>> resp = songController.getLyricsDirect("Adele", "Hello");

        verify(lyricsService).getLyrics("Hello", "Adele");
    }
}
