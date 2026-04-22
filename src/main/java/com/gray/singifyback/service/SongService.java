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
public class SongService {

    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final YtDlpService ytDlpService;

    public SongService(SongRepository songRepository, UserRepository userRepository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       YtDlpService ytDlpService) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.ytDlpService = ytDlpService;
    }

    @Transactional(readOnly = true)
    public List<SongResponse> getAllSongs(String userEmail) {
        User user = resolveUser(userEmail);
        return songRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(song -> toResponse(song, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SongResponse> searchSongs(String query, String userEmail) {
        User user = resolveUser(userEmail);
        return songRepository
                .findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(query, query)
                .stream()
                .map(song -> toResponse(song, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public SongResponse getSongById(String id, String userEmail) {
        User user = resolveUser(userEmail);
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + id));
        try {
            String payload = (userEmail != null ? userEmail : "anonymous") + ":" + id;
            kafkaTemplate.send(KafkaConfig.TOPIC_SONG_PLAYED, payload)
                    .whenComplete((result, ex) -> { /* silently ignore async failures */ });
        } catch (Exception ignored) {
            // Kafka not running locally — song response still returned
        }
        return toResponse(song, user);
    }

    private User resolveUser(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    private SongResponse toResponse(Song song, User user) {
        boolean liked = user != null && user.getLikedSongs().contains(song);
        String audioUrl = song.getAudioUrl();
        if (audioUrl == null || audioUrl.isBlank()) {
            audioUrl = ytDlpService.getProxyStreamUrl(song.getArtist(), song.getTitle());
        }
        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getCoverUrl(),
                audioUrl,
                song.getDuration(),
                liked,
                song.getPreviewUrl()
        );
    }
}
