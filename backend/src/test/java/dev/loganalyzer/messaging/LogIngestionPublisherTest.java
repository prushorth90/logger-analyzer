package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogIngestionAcceptedResponse;
import dev.loganalyzer.entity.Severity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogIngestionPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void publishesVersionedEventWithCorrelationId() {
        KafkaTemplate<String, LogRawEventV1> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, LogRawEventV1>> sendResult = new CompletableFuture<>();
        ArgumentCaptor<LogRawEventV1> eventCaptor = ArgumentCaptor.forClass(LogRawEventV1.class);
        LogIngestionPublisher publisher = new LogIngestionPublisher(kafkaTemplate);
        CreateLogEntryRequest request = new CreateLogEntryRequest(
                Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production", Severity.ERROR,
                "Payment failed", "trace-123", "billing-01", Map.of("durationMs", 42));
        when(kafkaTemplate.send(org.mockito.ArgumentMatchers.eq(LogIngestionPublisher.TOPIC),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(sendResult);

        LogIngestionAcceptedResponse response = publisher.publish(request, "correlation-123");

        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq(LogIngestionPublisher.TOPIC),
                org.mockito.ArgumentMatchers.eq(response.eventId().toString()), eventCaptor.capture());
        assertThat(response.correlationId()).isEqualTo("correlation-123");
        assertThat(response.status()).isEqualTo("accepted");
        assertThat(eventCaptor.getValue().schemaVersion()).isEqualTo(LogRawEventV1.SCHEMA_VERSION);
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(response.eventId());
        assertThat(eventCaptor.getValue().correlationId()).isEqualTo("correlation-123");
        assertThat(eventCaptor.getValue().metadata()).containsEntry("durationMs", 42);
    }
}