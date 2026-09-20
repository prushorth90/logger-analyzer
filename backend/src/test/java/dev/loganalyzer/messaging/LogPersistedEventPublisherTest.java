package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.observability.ApplicationMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogPersistedEventPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void publishesPersistedEventWithEventIdAsKey() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ApplicationMetrics metrics = mock(ApplicationMetrics.class);
        LogPersistedEventPublisher publisher = new LogPersistedEventPublisher(kafkaTemplate, metrics);
        LogPersistedEventV1 event = new LogPersistedEventV1(LogPersistedEventV1.SCHEMA_VERSION,
                UUID.randomUUID(), Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production",
                Severity.ERROR, "Payment failed", "trace-123");
            when(kafkaTemplate.send(LogPersistedEventPublisher.TOPIC, event.eventId().toString(), event))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        verify(kafkaTemplate).send(LogPersistedEventPublisher.TOPIC, event.eventId().toString(), event);
        verify(metrics).persistedMessagePublished();
    }
}