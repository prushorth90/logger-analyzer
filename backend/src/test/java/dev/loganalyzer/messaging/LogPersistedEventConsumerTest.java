package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.search.OpenSearchLogIndex;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LogPersistedEventConsumerTest {
    @Test
    void indexesPersistedEvent() {
        OpenSearchLogIndex logIndex = mock(OpenSearchLogIndex.class);
        LogPersistedEventConsumer consumer = new LogPersistedEventConsumer(logIndex);
        LogPersistedEventV1 event = new LogPersistedEventV1(LogPersistedEventV1.SCHEMA_VERSION,
                UUID.randomUUID(), Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production",
                Severity.ERROR, "Payment failed", "trace-123");

        consumer.consume(event);

        verify(logIndex).index(event);
    }
}