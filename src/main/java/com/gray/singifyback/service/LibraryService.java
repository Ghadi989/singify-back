package com.gray.singifyback.service;

import com.gray.singifyback.config.KafkaConfig;
import com.gray.singifyback.dto.request.LikeRequest;
import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
                        song.getCoverUrl(), song.getAudioUrl(), song.getDuration(), true, song.getPreviewUrl()))
                .toList();
    }

    @Transactional
    public String likeSong(String userEmail, String songId, LikeRequest meta) {
        User user = getUser(userEmail);
        Song song = findOrCreateSong(songId, meta);
        if (!user.getLikedSongs().contains(song)) {
            user.getLikedSongs().add(song);
            userRepository.save(user);
        }
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_SONG_LIKED, userEmail + ":" + song.getId())
                    .whenComplete((result, ex) -> { /* silently ignore async failures */ });
        } catch (Exception ignored) {
            // Kafka not running locally — like is already saved above
        }
        return song.getId();
    }

    @Transactional
    public void unlikeSong(String userEmail, String songId) {
        User user = getUser(userEmail);
        Song song = resolveSong(songId);
        user.getLikedSongs().remove(song);
        userRepository.save(user);
    }

    // If not found by primary key, tries spotifyId for "spotify-<trackId>" synthetic IDs.
    // If still not found and metadata is provided, creates and persists the song.
    private Song findOrCreateSong(String id, LikeRequest meta) {
        try {
            return resolveSong(id);
        } catch (IllegalArgumentException e) {
            if (meta != null && meta.title() != null && meta.artist() != null) {
                Song song = new Song();
                song.setTitle(meta.title());
                song.setArtist(meta.artist());
                song.setCoverUrl(meta.coverUrl());
                song.setDuration(meta.duration());
                song.setSpotifyId(meta.spotifyId());
                song.setPreviewUrl(meta.previewUrl());
                return songRepository.save(song);
            }
            throw e;
        }
    }

    private Song resolveSong(String id) {
        Optional<Song> byId = songRepository.findById(id);
        if (byId.isPresent()) return byId.get();
        if (id.startsWith("spotify-")) {
            return songRepository.findBySpotifyId(id.substring(8))
                    .orElseThrow(() -> new IllegalArgumentException("Song not found: " + id));
        }
        throw new IllegalArgumentException("Song not found: " + id);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }
}
