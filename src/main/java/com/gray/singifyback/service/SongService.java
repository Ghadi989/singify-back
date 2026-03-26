package com.gray.singifyback.service;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    private final SongRepository songRepository;
    private final UserRepository userRepository;

    public SongService(SongRepository songRepository, UserRepository userRepository) {
        this.songRepository = songRepository;
        this.userRepository = userRepository;
    }

    public List<SongResponse> getAllSongs(String userEmail) {
        User user = resolveUser(userEmail);
        return songRepository.findAll().stream()
                .map(song -> toResponse(song, user))
                .toList();
    }

    public List<SongResponse> searchSongs(String query, String userEmail) {
        User user = resolveUser(userEmail);
        return songRepository
                .findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(query, query)
                .stream()
                .map(song -> toResponse(song, user))
                .toList();
    }

    public SongResponse getSongById(String id, String userEmail) {
        User user = resolveUser(userEmail);
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + id));
        return toResponse(song, user);
    }

    private User resolveUser(String email) {
        if (email == null) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    private SongResponse toResponse(Song song, User user) {
        boolean liked = user != null && user.getLikedSongs().contains(song);
        return new SongResponse(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getCoverUrl(),
                song.getAudioUrl(),
                song.getDuration(),
                liked
        );
    }
}
