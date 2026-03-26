package com.gray.singifyback.unit;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import com.gray.singifyback.service.SongService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock SongRepository songRepository;
    @Mock UserRepository userRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks SongService songService;

    @Test
    void getAllSongs_unauthenticated_returnsAllSongsWithLikedFalse() {
        Song song = new Song("id-1", "Hard Times", "Paramore", null, "http://audio.url", "2:57");
        when(songRepository.findAll()).thenReturn(List.of(song));

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
}
