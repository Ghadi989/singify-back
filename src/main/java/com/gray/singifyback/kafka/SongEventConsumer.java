package com.gray.singifyback.kafka;

import com.gray.singifyback.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SongEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SongEventConsumer.class);

    @KafkaListener(topics = KafkaConfig.TOPIC_SONG_LIKED, groupId = "singify-group")
    public void onSongLiked(String message) {
        // message format: "userEmail:songId"
        log.info("[Kafka] Song liked event received: {}", message);
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_SONG_PLAYED, groupId = "singify-group")
    public void onSongPlayed(String message) {
        // message format: "userEmail:songId"
        log.info("[Kafka] Song played event received: {}", message);
    }
}
