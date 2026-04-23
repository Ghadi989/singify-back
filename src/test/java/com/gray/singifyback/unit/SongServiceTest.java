package com.gray.singifyback.unit;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import com.gray.singifyback.service.SongService;
import com.gray.singifyback.service.YtDlpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.gray.singifyback.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock SongRepository songRepository;
    @Mock UserRepository userRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock YtDlpService ytDlpService;

    @InjectMocks SongService songService;

    @Test
    void getAllSongs_unauthenticated_returnsAllSongsWithLikedFalse() {
        Song song = new Song("id-1", "Hard Times", "Paramore", null, "http://audio.url", "2:57");
        when(songRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(song));

        List<SongResponse> result = songService.getAllSongs(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Hard Times");
        assertThat(result.get(0).userLike()).isFalse();
    }

    @Test
    void searchSongs_matchingQuery_returnsResults() {
        Song song = new Song("id-1", "Bad Guy", "Billie Eilish", null, "http://audio.url", "3:14");
        when(songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase("billie", "billie"))
                .thenReturn(List.of(song));

        List<SongResponse> result = songService.searchSongs("billie", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).artist()).isEqualTo("Billie Eilish");
    }

    @Test
    void getSongById_existingSong_returnsResponse() {
        Song song = new Song("id-1", "Shape of You", "Ed Sheeran", null, "http://audio.url", "3:53");
        when(songRepository.findById("id-1")).thenReturn(Optional.of(song));

        // Kafka.send() returns null from mock → .whenComplete() throws NPE → caught by try-catch
        SongResponse result = songService.getSongById("id-1", null);

        assertThat(result.title()).isEqualTo("Shape of You");
        assertThat(result.userLike()).isFalse();
    }

    @Test
    void getSongById_notFound_throwsException() {
        when(songRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> songService.getSongById("missing", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Song not found");
    }

    @Test
    void searchSongs_emptyResult_returnsEmptyList() {
        when(songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase("xyz", "xyz"))
                .thenReturn(List.of());

        List<SongResponse> result = songService.searchSongs("xyz", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllSongs_withAuthenticatedUser_setsUserLikeCorrectly() {
        Song liked = new Song("id-1", "Hello", "Adele", null, "http://audio.url", "4:55");
        Song notLiked = new Song("id-2", "Rolling in the Deep", "Adele", null, "http://audio.url", "3:48");

        User user = new User();
        user.setId("u1");
        user.setEmail("user@example.com");
        user.setLikedSongs(Set.of(liked));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(songRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(liked, notLiked));

        List<SongResponse> result = songService.getAllSongs("user@example.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userLike()).isTrue();
        assertThat(result.get(1).userLike()).isFalse();
    }

    @Test
    void getAllSongs_noAudioUrl_callsYtDlpForProxyUrl() {
        Song s = new Song("id-1", "Hello", "Adele", null, null, "4:55");
        when(songRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(s));
        when(ytDlpService.getProxyStreamUrl("Adele", "Hello")).thenReturn("http://proxy/stream");

        List<SongResponse> result = songService.getAllSongs(null);

        assertThat(result.get(0).audioUrl()).isEqualTo("http://proxy/stream");
        verify(ytDlpService).getProxyStreamUrl("Adele", "Hello");
    }
}
