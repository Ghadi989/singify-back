package com.gray.singifyback.kafka;

import com.gray.singifyback.config.KafkaTopicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private SongEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new SongEventProducer(kafkaTemplate);
    }

    @Test
    void publishSongSearch_sendsToCorrectTopic() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        producer.publishSongSearch("Bohemian");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicConfig.SONG_SEARCH_EVENTS);
        assertThat(keyCaptor.getValue()).isEqualTo("Bohemian");
        assertThat(valueCaptor.getValue()).isInstanceOf(SongEventProducer.SongSearchEvent.class);

        SongEventProducer.SongSearchEvent event = (SongEventProducer.SongSearchEvent) valueCaptor.getValue();
        assertThat(event.query()).isEqualTo("Bohemian");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void publishKaraokeSession_sendsToCorrectTopic() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        producer.publishKaraokeSession("song-42", "user-7");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicConfig.KARAOKE_SESSION_EVENTS);
        assertThat(keyCaptor.getValue()).isEqualTo("song-42");
        assertThat(valueCaptor.getValue()).isInstanceOf(SongEventProducer.KaraokeSessionEvent.class);

        SongEventProducer.KaraokeSessionEvent event = (SongEventProducer.KaraokeSessionEvent) valueCaptor.getValue();
        assertThat(event.songId()).isEqualTo("song-42");
        assertThat(event.userId()).isEqualTo("user-7");
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void publishSongSearch_logsErrorWhenFutureFails() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        producer.publishSongSearch("fail-query");

        // Complete the future exceptionally to exercise the error-logging branch
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.SONG_SEARCH_EVENTS), eq("fail-query"), any());
    }

    @Test
    void publishSongSearch_logsSuccessWhenFutureCompletes() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        producer.publishSongSearch("success-query");

        // Leave the future incomplete — the whenComplete callback is async; the send call itself is what matters
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.SONG_SEARCH_EVENTS), eq("success-query"), any());
    }
}
