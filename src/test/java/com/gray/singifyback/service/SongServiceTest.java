package com.gray.singifyback.service;

import com.gray.singifyback.dto.SongCreateDTO;
import com.gray.singifyback.dto.SongDTO;
import com.gray.singifyback.exception.ResourceNotFoundException;
import com.gray.singifyback.kafka.SongEventProducer;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private SongEventProducer songEventProducer;

    @InjectMocks
    private SongService songService;

    private Song song;
    private SongCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        song = Song.builder()
                .id(1L)
                .spotifyId("spotify-123")
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .genre("Rock")
                .releaseYear(1975)
                .albumCoverUrl("http://example.com/cover.jpg")
                .durationMs(354000L)
                .build();

        createDTO = SongCreateDTO.builder()
                .spotifyId("spotify-123")
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .genre("Rock")
                .releaseYear(1975)
                .albumCoverUrl("http://example.com/cover.jpg")
                .durationMs(354000L)
                .build();
    }

    @Test
    void getAllSongs_returnsMappedList() {
        when(songRepository.findAll()).thenReturn(List.of(song));

        List<SongDTO> result = songService.getAllSongs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(result.get(0).getArtist()).isEqualTo("Queen");
    }

    @Test
    void getSongById_returnsCorrectSong() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(song));

        SongDTO result = songService.getSongById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(result.getSpotifyId()).isEqualTo("spotify-123");
    }

    @Test
    void getSongById_throwsResourceNotFoundWhenMissing() {
        when(songRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> songService.getSongById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createSong_savesAndReturnsDTO() {
        when(songRepository.save(any(Song.class))).thenReturn(song);

        SongDTO result = songService.createSong(createDTO);

        assertThat(result.getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(result.getGenre()).isEqualTo("Rock");
        verify(songRepository).save(any(Song.class));
    }

    @Test
    void searchByTitle_callsProducerAndReturnsResults() {
        when(songRepository.findByTitleContainingIgnoreCase("Bohemian"))
                .thenReturn(List.of(song));

        List<SongDTO> result = songService.searchByTitle("Bohemian");

        assertThat(result).hasSize(1);
        verify(songEventProducer).publishSongSearch("Bohemian");
    }

    @Test
    void searchByArtist_callsProducerAndReturnsResults() {
        when(songRepository.findByArtistContainingIgnoreCase("Queen"))
                .thenReturn(List.of(song));

        List<SongDTO> result = songService.searchByArtist("Queen");

        assertThat(result).hasSize(1);
        verify(songEventProducer).publishSongSearch("Queen");
    }

    @Test
    void deleteSong_callsRepositoryDeleteById() {
        when(songRepository.existsById(1L)).thenReturn(true);

        songService.deleteSong(1L);

        verify(songRepository).deleteById(1L);
    }

    @Test
    void deleteSong_throwsResourceNotFoundWhenMissing() {
        when(songRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> songService.deleteSong(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(songRepository, never()).deleteById(any());
    }

    @Test
    void updateSong_updatesFieldsAndReturnsDTO() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(song));
        when(songRepository.save(any(Song.class))).thenReturn(song);

        SongDTO result = songService.updateSong(1L, createDTO);

        assertThat(result.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(songRepository).save(any(Song.class));
    }

    @Test
    void filterByGenre_returnsMappedList() {
        when(songRepository.findByGenre("Rock")).thenReturn(List.of(song));

        List<SongDTO> result = songService.filterByGenre("Rock");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGenre()).isEqualTo("Rock");
    }

    @Test
    void filterByYear_returnsMappedList() {
        when(songRepository.findByReleaseYear(1975)).thenReturn(List.of(song));

        List<SongDTO> result = songService.filterByYear(1975);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReleaseYear()).isEqualTo(1975);
    }
}
