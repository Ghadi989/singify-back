package com.gray.singifyback.repository;

import com.gray.singifyback.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findByTitleContainingIgnoreCase(String title);

    List<Song> findByArtistContainingIgnoreCase(String artist);

    List<Song> findByGenre(String genre);

    List<Song> findByReleaseYear(Integer releaseYear);

    Optional<Song> findBySpotifyId(String spotifyId);
}
