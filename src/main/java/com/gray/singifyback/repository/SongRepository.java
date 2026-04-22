package com.gray.singifyback.repository;

import com.gray.singifyback.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, String> {

    List<Song> findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(String title, String artist);

    Optional<Song> findBySpotifyId(String spotifyId);

    /** Returns all songs ordered by most-recently processed first. */
    List<Song> findAllByOrderByCreatedAtDesc();
}
