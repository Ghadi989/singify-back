package com.gray.singifyback.unit;

import com.gray.singifyback.dto.request.LikeRequest;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import com.gray.singifyback.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock UserRepository userRepository;
    @Mock SongRepository songRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks LibraryService libraryService;

    private User userWithEmail(String email) {
        User u = new User();
        u.setId("u1");
        u.setEmail(email);
        u.setUsername("testuser");
        return u;
    }

    private Song song(String id, String title, String artist) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        s.setArtist(artist);
        s.setAudioUrl("http://audio.example.com");
        return s;
    }

    @Test
    void getUserLibrary_returnsLikedSongs() {
        User user = userWithEmail("a@b.com");
        Song s = song("s1", "Hello", "Adele");
        user.getLikedSongs().add(s);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        List<SongResponse> result = libraryService.getLikedSongs("a@b.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("s1");
        assertThat(result.get(0).userLike()).isTrue();
    }

    @Test
    void addToLibrary_success_savesSongToUser() {
        User user = userWithEmail("a@b.com");
        Song s = song("s1", "Hello", "Adele");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(songRepository.findById("s1")).thenReturn(Optional.of(s));
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("no kafka"));

        libraryService.likeSong("a@b.com", "s1", null);

        assertThat(user.getLikedSongs()).contains(s);
        verify(userRepository).save(user);
    }

    @Test
    void addToLibrary_alreadyLiked_doesNotSaveAgain() {
        User user = userWithEmail("a@b.com");
        Song s = song("s1", "Hello", "Adele");
        user.getLikedSongs().add(s);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(songRepository.findById("s1")).thenReturn(Optional.of(s));
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("no kafka"));

        libraryService.likeSong("a@b.com", "s1", null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void removeFromLibrary_success_removesSongFromUser() {
        User user = userWithEmail("a@b.com");
        Song s = song("s1", "Hello", "Adele");
        user.getLikedSongs().add(s);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(songRepository.findById("s1")).thenReturn(Optional.of(s));

        libraryService.unlikeSong("a@b.com", "s1");

        assertThat(user.getLikedSongs()).doesNotContain(s);
        verify(userRepository).save(user);
    }

    @Test
    void addToLibrary_songNotFound_withMeta_createsAndSavesSong() {
        User user = userWithEmail("a@b.com");
        LikeRequest meta = new LikeRequest("Rolling in the Deep", "Adele", null, "3:48", "spotId1", null);
        Song created = song("newId", meta.title(), meta.artist());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(songRepository.findById("newId")).thenReturn(Optional.empty());
        when(songRepository.findById("newId")).thenReturn(Optional.empty());
        // findById for "newId" returns empty, then findOrCreate falls through to save
        when(songRepository.save(any(Song.class))).thenReturn(created);
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenThrow(new RuntimeException("no kafka"));

        libraryService.likeSong("a@b.com", "newId", meta);

        verify(songRepository).save(any(Song.class));
    }
}
