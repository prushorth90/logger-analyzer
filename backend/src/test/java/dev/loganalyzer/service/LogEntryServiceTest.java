package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogEntryServiceTest {
    @Test
    void persistsConsumedLogEvent() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        LogEntryService service = new LogEntryService(repository);
        Instant timestamp = Instant.parse("2026-09-17T12:00:00Z");
        Map<String, Object> metadata = Map.of("requestMethod", "POST", "durationMs", 42);
        UUID eventId = UUID.randomUUID();
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, eventId, "correlation-123",
                timestamp, "billing-api", "production", Severity.ERROR, "Payment failed", "trace-123",
                "billing-01", metadata);

        service.persist(event);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIngestionEventId()).isEqualTo(eventId);
        assertThat(captor.getValue().getServiceName()).isEqualTo("billing-api");
        assertThat(captor.getValue().getTimestamp()).isEqualTo(timestamp);
        assertThat(captor.getValue().getMetadata()).isEqualTo(metadata);
    }

    @Test
    void ignoresAlreadyPersistedEvent() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        LogEntryService service = new LogEntryService(repository);
        UUID eventId = UUID.randomUUID();
        when(repository.existsByIngestionEventId(eventId)).thenReturn(true);
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, eventId, "correlation-123",
                Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production", Severity.ERROR,
                "Payment failed", null, "billing-01", Map.of());

        service.persist(event);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEmptyWhenLogEntryDoesNotExist() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        LogEntryService service = new LogEntryService(repository);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThat(service.findById(id)).isEmpty();
        verify(repository).findById(id);
    }
}