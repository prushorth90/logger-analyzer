package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.messaging.LogPersistedEventV1;
import dev.loganalyzer.repository.LogEntryRepository;
import dev.loganalyzer.search.LogSearchResult;
import dev.loganalyzer.search.LogSearchQueryParser;
import dev.loganalyzer.search.OpenSearchLogIndex;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class LogEntryServiceTest {
    @Test
    void persistsConsumedLogEvent() throws Exception {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        LogEntryService service = new LogEntryService(repository, new ObjectMapper(), meterRegistry, eventPublisher,
            mock(OpenSearchLogIndex.class), new LogSearchQueryParser());
        Instant timestamp = Instant.parse("2026-09-17T12:00:00Z");
        Map<String, Object> metadata = Map.of("requestMethod", "POST", "durationMs", 42);
        UUID eventId = UUID.randomUUID();
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, eventId, "correlation-123",
                timestamp, "billing-api", "production", Severity.ERROR, "Payment failed", "trace-123",
                "billing-01", metadata);
            when(repository.insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        service.persist(event);

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).insertIfAbsent(any(), org.mockito.ArgumentMatchers.eq(eventId),
                org.mockito.ArgumentMatchers.eq(timestamp), org.mockito.ArgumentMatchers.eq("billing-api"),
                org.mockito.ArgumentMatchers.eq("production"), org.mockito.ArgumentMatchers.eq("ERROR"),
                org.mockito.ArgumentMatchers.eq("Payment failed"), org.mockito.ArgumentMatchers.eq("trace-123"),
                org.mockito.ArgumentMatchers.eq("billing-01"),
                metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).isEqualTo(new ObjectMapper().writeValueAsString(metadata));
        assertThat(meterRegistry.counter("log.ingestion.duplicates").count()).isZero();
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.isA(LogPersistedEventV1.class));
    }

    @Test
    void ignoresAlreadyPersistedEvent() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        LogEntryService service = new LogEntryService(repository, new ObjectMapper(), meterRegistry, eventPublisher,
            mock(OpenSearchLogIndex.class), new LogSearchQueryParser());
        UUID eventId = UUID.randomUUID();
        when(repository.insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(0);
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, eventId, "correlation-123",
                Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production", Severity.ERROR,
                "Payment failed", null, "billing-01", Map.of());

        service.persist(event);

        assertThat(meterRegistry.counter("log.ingestion.duplicates").count()).isEqualTo(1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void returnsEmptyWhenLogEntryDoesNotExist() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        LogEntryService service = service(repository, mock(OpenSearchLogIndex.class));
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThat(service.findById(id)).isEmpty();
        verify(repository).findById(id);
    }

    @Test
    void hydratesOpenSearchMatchesFromPostgresqlInHitOrder() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        OpenSearchLogIndex logIndex = mock(OpenSearchLogIndex.class);
        LogEntryService service = service(repository, logIndex);
        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        dev.loganalyzer.entity.LogEntry first = log(firstEventId, "First match");
        dev.loganalyzer.entity.LogEntry second = log(secondEventId, "Second match");
        when(logIndex.search(any(), any())).thenReturn(new LogSearchResult(List.of(firstEventId, secondEventId), 2));
        when(repository.findByIngestionEventIdIn(List.of(firstEventId, secondEventId)))
                .thenReturn(List.of(second, first));

        var result = service.findAll(null, null, null, null, null, null, "payment",
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp")));

        assertThat(result.content()).extracting("message").containsExactly("First match", "Second match");
        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.queryExecutionMs()).isGreaterThanOrEqualTo(0);
    }

    private LogEntryService service(LogEntryRepository repository, OpenSearchLogIndex logIndex) {
        return new LogEntryService(repository, new ObjectMapper(), new SimpleMeterRegistry(),
            mock(ApplicationEventPublisher.class), logIndex, new LogSearchQueryParser());
    }

    private dev.loganalyzer.entity.LogEntry log(UUID eventId, String message) {
        return new dev.loganalyzer.entity.LogEntry(eventId, Instant.parse("2026-09-17T12:00:00Z"), "billing-api",
                "production", Severity.ERROR, message, null, "billing-01", Map.of());
    }
}