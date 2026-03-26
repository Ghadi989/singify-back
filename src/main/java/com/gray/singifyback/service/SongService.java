package com.gray.singifyback.service;

import com.gray.singifyback.dto.response.SongResponse;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.model.User;
import com.gray.singifyback.repository.SongRepository;
import com.gray.singifyback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;
    private final UserRepository userRepository;

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
        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .coverUrl(song.getCoverUrl())
                .audioUrl(song.getAudioUrl())
                .duration(song.getDuration())
                .userLike(liked)
                .build();
    }
}
