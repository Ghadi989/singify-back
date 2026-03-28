package com.gray.singifyback.kafka;

import com.gray.singifyback.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongEventProducer {

    public record SongSearchEvent(String query, Instant timestamp) {}

    public record KaraokeSessionEvent(String songId, String userId, Instant timestamp) {}

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSongSearch(String query) {
        SongSearchEvent event = new SongSearchEvent(query, Instant.now());
        kafkaTemplate.send(KafkaTopicConfig.SONG_SEARCH_EVENTS, query, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SongSearchEvent for query '{}': {}", query, ex.getMessage());
                    } else {
                        log.debug("Published SongSearchEvent for query '{}' to partition {} offset {}",
                                query,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishKaraokeSession(String songId, String userId) {
        KaraokeSessionEvent event = new KaraokeSessionEvent(songId, userId, Instant.now());
        kafkaTemplate.send(KafkaTopicConfig.KARAOKE_SESSION_EVENTS, songId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish KaraokeSessionEvent for song '{}' user '{}': {}",
                                songId, userId, ex.getMessage());
                    } else {
                        log.debug("Published KaraokeSessionEvent for song '{}' user '{}' to partition {} offset {}",
                                songId,
                                userId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
