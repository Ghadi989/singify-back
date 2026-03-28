package com.gray.singifyback.kafka;

import com.gray.singifyback.config.KafkaTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SongEventConsumer {

    @KafkaListener(
            topics = KafkaTopicConfig.SONG_SEARCH_EVENTS,
            groupId = "singify-group"
    )
    public void consumeSongSearchEvent(@Payload Object event) {
        log.info("Received message from '{}': {}", KafkaTopicConfig.SONG_SEARCH_EVENTS, event);
        // TODO: implement business logic for song search events
    }

    @KafkaListener(
            topics = KafkaTopicConfig.KARAOKE_SESSION_EVENTS,
            groupId = "singify-group"
    )
    public void consumeKaraokeSessionEvent(@Payload Object event) {
        log.info("Received message from '{}': {}", KafkaTopicConfig.KARAOKE_SESSION_EVENTS, event);
        // TODO: implement business logic for karaoke session events
    }

    @KafkaListener(
            topics = KafkaTopicConfig.LYRICS_READY_EVENTS,
            groupId = "singify-group"
    )
    public void consumeLyricsReadyEvent(@Payload Object event) {
        log.info("Received message from '{}': {}", KafkaTopicConfig.LYRICS_READY_EVENTS, event);
        // TODO: implement business logic for lyrics-ready events
    }
}
