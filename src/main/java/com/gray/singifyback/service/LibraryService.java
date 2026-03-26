package com.gray.singifyback.service;

import com.gray.singifyback.config.KafkaConfig;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public List<SongResponse> getLikedSongs(String userEmail) {
        User user = getUser(userEmail);
        return user.getLikedSongs().stream()
                .map(song -> SongResponse.builder()
                        .id(song.getId())
                        .title(song.getTitle())
                        .artist(song.getArtist())
                        .coverUrl(song.getCoverUrl())
                        .audioUrl(song.getAudioUrl())
                        .duration(song.getDuration())
                        .userLike(true)
                        .build())
                .toList();
    }

    @Transactional
    public void likeSong(String userEmail, String songId) {
        User user = getUser(userEmail);
        Song song = getSong(songId);
        user.getLikedSongs().add(song);
        userRepository.save(user);
        kafkaTemplate.send(KafkaConfig.TOPIC_SONG_LIKED, userEmail + ":" + songId);
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
