package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.entity.DeadLetterEvent;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogIngestionPublisher;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.repository.DeadLetterEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadLetterEventServiceTest {
    @Test
    void storesOriginalEventAndFailureDetailsOnce() {
        DeadLetterEventRepository repository = mock(DeadLetterEventRepository.class);
        LogIngestionPublisher publisher = mock(LogIngestionPublisher.class);
        DeadLetterEventService service = new DeadLetterEventService(repository, publisher);
        LogRawEventV1 event = event();
        Instant failedAt = Instant.parse("2026-09-18T12:00:00Z");
        when(repository.findByIngestionEventId(event.eventId())).thenReturn(Optional.empty());

        service.record(event, "database unavailable", 2, failedAt);

        ArgumentCaptor<DeadLetterEvent> captor = ArgumentCaptor.forClass(DeadLetterEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOriginalEvent()).isEqualTo(event);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("database unavailable");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(2);
        assertThat(captor.getValue().getFailedAt()).isEqualTo(failedAt);
    }

    @Test
    void capsManualRetriesToPreventReplayLoops() {
        DeadLetterEventRepository repository = mock(DeadLetterEventRepository.class);
        LogIngestionPublisher publisher = mock(LogIngestionPublisher.class);
        DeadLetterEventService service = new DeadLetterEventService(repository, publisher);
        LogRawEventV1 original = event();
        DeadLetterEvent deadLetter = new DeadLetterEvent(original, "database unavailable", 2, Instant.now());
        when(repository.findByIngestionEventId(original.eventId())).thenReturn(Optional.of(deadLetter));

        service.retry(original.eventId());
        service.retry(original.eventId());
        service.retry(original.eventId());

        assertThatThrownBy(() -> service.retry(original.eventId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verify(publisher, times(3)).republish(original);
    }

    @Test
    void doesNotStoreDuplicateDeadLetterDelivery() {
        DeadLetterEventRepository repository = mock(DeadLetterEventRepository.class);
        DeadLetterEventService service = new DeadLetterEventService(repository, mock(LogIngestionPublisher.class));
        LogRawEventV1 event = event();
        when(repository.findByIngestionEventId(event.eventId()))
                .thenReturn(Optional.of(new DeadLetterEvent(event, "failure", 2, Instant.now())));

        service.record(event, "failure", 2, Instant.now());

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private LogRawEventV1 event() {
        return new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, UUID.randomUUID(), "correlation-123",
                Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "production", Severity.ERROR,
                "Payment failed", "trace-123", "billing-01", Map.of("provider", "example-pay"));
    }
}