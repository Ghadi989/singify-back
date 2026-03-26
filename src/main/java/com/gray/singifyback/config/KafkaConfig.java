package com.gray.singifyback.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_SONG_PLAYED = "song.played";
    public static final String TOPIC_SONG_LIKED  = "song.liked";

    @Bean
    public NewTopic songPlayedTopic() {
        return TopicBuilder.name(TOPIC_SONG_PLAYED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic songLikedTopic() {
        return TopicBuilder.name(TOPIC_SONG_LIKED).partitions(1).replicas(1).build();
    }
}
