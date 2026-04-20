package com.gray.singifyback.repository;

import com.gray.singifyback.model.Lyrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LyricsRepository extends JpaRepository<Lyrics, Long> {

    Optional<Lyrics> findBySongId(Long songId);
}
