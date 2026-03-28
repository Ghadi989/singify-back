package com.gray.singifyback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gray.singifyback.config.KafkaTopicConfig;
import com.gray.singifyback.dto.LyricsDTO;
import com.gray.singifyback.model.Lyrics;
import com.gray.singifyback.exception.ResourceNotFoundException;
import com.gray.singifyback.repository.LyricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LyricsService {

    private final LyricsRepository lyricsRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public LyricsDTO getLyricsBySongId(Long songId) {
        Lyrics lyrics = lyricsRepository.findBySongId(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Lyrics not found for songId: " + songId));
        return toDTO(lyrics);
    }

    @Transactional
    public LyricsDTO saveLyrics(LyricsDTO dto) {
        String linesJson = serializeLines(dto.lines());

        Lyrics lyrics = lyricsRepository.findBySongId(dto.songId())
                .orElseGet(Lyrics::new);

        lyrics.setSongId(dto.songId());
        lyrics.setLines(linesJson);
        lyrics.setSource(dto.source());

        Lyrics saved = lyricsRepository.save(lyrics);

        kafkaTemplate.send(KafkaTopicConfig.LYRICS_READY_EVENTS, String.valueOf(dto.songId()), dto)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish lyrics-ready event for songId {}: {}", dto.songId(), ex.getMessage());
                    } else {
                        log.debug("Published lyrics-ready event for songId {} to partition {} offset {}",
                                dto.songId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        return toDTO(saved);
    }

    private String serializeLines(List<LyricsDTO.LyricLine> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize lyric lines", e);
        }
    }

    private List<LyricsDTO.LyricLine> deserializeLines(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LyricsDTO.LyricLine.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize lyric lines", e);
        }
    }

    private LyricsDTO toDTO(Lyrics lyrics) {
        List<LyricsDTO.LyricLine> lines = lyrics.getLines() != null
                ? deserializeLines(lyrics.getLines())
                : List.of();
        return new LyricsDTO(lyrics.getSongId(), lines, lyrics.getSource());
    }
}
