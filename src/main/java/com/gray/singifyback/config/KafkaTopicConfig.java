package com.gray.singifyback.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SONG_SEARCH_EVENTS      = "song-search-events";
    public static final String KARAOKE_SESSION_EVENTS  = "karaoke-session-events";
    public static final String LYRICS_READY_EVENTS     = "lyrics-ready-events";

    @Bean
    public NewTopic songSearchEventsTopic() {
        return TopicBuilder.name(SONG_SEARCH_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic karaokeSessionEventsTopic() {
        return TopicBuilder.name(KARAOKE_SESSION_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic lyricsReadyEventsTopic() {
        return TopicBuilder.name(LYRICS_READY_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
