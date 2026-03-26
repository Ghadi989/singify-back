package com.gray.singifyback.service;

import com.gray.singifyback.config.KafkaConfig;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LibraryService {

    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public LibraryService(UserRepository userRepository, SongRepository songRepository,
                          KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository = userRepository;
        this.songRepository = songRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<SongResponse> getLikedSongs(String userEmail) {
        User user = getUser(userEmail);
        return user.getLikedSongs().stream()
                .map(song -> new SongResponse(
                        song.getId(), song.getTitle(), song.getArtist(),
                        song.getCoverUrl(), song.getAudioUrl(), song.getDuration(), true))
                .toList();
    }

    @Transactional
    public void likeSong(String userEmail, String songId) {
        User user = getUser(userEmail);
        Song song = getSong(songId);
        user.getLikedSongs().add(song);
        userRepository.save(user);
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_SONG_LIKED, userEmail + ":" + songId);
        } catch (Exception e) {
            // Kafka not available locally — like still saved in DB
        }
    }

    @Transactional
    public void unlikeSong(String userEmail, String songId) {
        User user = getUser(userEmail);
        Song song = getSong(songId);
        user.getLikedSongs().remove(song);
        userRepository.save(user);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    private Song getSong(String id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + id));
    }
}
