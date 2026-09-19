package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LogPersistedEventPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void publishesPersistedEventWithEventIdAsKey() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        LogPersistedEventPublisher publisher = new LogPersistedEventPublisher(kafkaTemplate);
        LogPersistedEventV1 event = new LogPersistedEventV1(LogPersistedEventV1.SCHEMA_VERSION,
                UUID.randomUUID(), Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production",
                Severity.ERROR, "Payment failed", "trace-123");

        publisher.publish(event);

        verify(kafkaTemplate).send(LogPersistedEventPublisher.TOPIC, event.eventId().toString(), event);
    }
}