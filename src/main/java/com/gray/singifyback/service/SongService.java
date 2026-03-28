package com.gray.singifyback.service;

import com.gray.singifyback.dto.SongCreateDTO;
import com.gray.singifyback.dto.SongDTO;
import com.gray.singifyback.kafka.SongEventProducer;
import com.gray.singifyback.model.Song;
import com.gray.singifyback.exception.ResourceNotFoundException;
import com.gray.singifyback.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private final SongRepository songRepository;
    private final SongEventProducer songEventProducer;

    public List<SongDTO> getAllSongs() {
        return songRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public SongDTO getSongById(Long id) {
        return songRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));
    }

    public List<SongDTO> searchByTitle(String title) {
        songEventProducer.publishSongSearch(title);
        return songRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<SongDTO> searchByArtist(String artist) {
        songEventProducer.publishSongSearch(artist);
        return songRepository.findByArtistContainingIgnoreCase(artist).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<SongDTO> filterByGenre(String genre) {
        return songRepository.findByGenre(genre).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<SongDTO> filterByYear(Integer year) {
        return songRepository.findByReleaseYear(year).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public SongDTO createSong(SongCreateDTO dto) {
        Song song = toEntity(dto);
        return toDTO(songRepository.save(song));
    }

    @Transactional
    public SongDTO updateSong(Long id, SongCreateDTO dto) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));
        song.setSpotifyId(dto.getSpotifyId());
        song.setTitle(dto.getTitle());
        song.setArtist(dto.getArtist());
        song.setGenre(dto.getGenre());
        song.setReleaseYear(dto.getReleaseYear());
        song.setAlbumCoverUrl(dto.getAlbumCoverUrl());
        song.setDurationMs(dto.getDurationMs());
        return toDTO(songRepository.save(song));
    }

    @Transactional
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new ResourceNotFoundException("Song not found with id: " + id);
        }
        songRepository.deleteById(id);
    }

    private SongDTO toDTO(Song song) {
        return SongDTO.builder()
                .id(song.getId())
                .spotifyId(song.getSpotifyId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .genre(song.getGenre())
                .releaseYear(song.getReleaseYear())
                .albumCoverUrl(song.getAlbumCoverUrl())
                .durationMs(song.getDurationMs())
                .build();
    }

    private Song toEntity(SongCreateDTO dto) {
        return Song.builder()
                .spotifyId(dto.getSpotifyId())
                .title(dto.getTitle())
                .artist(dto.getArtist())
                .genre(dto.getGenre())
                .releaseYear(dto.getReleaseYear())
                .albumCoverUrl(dto.getAlbumCoverUrl())
                .durationMs(dto.getDurationMs())
                .build();
    }
}
