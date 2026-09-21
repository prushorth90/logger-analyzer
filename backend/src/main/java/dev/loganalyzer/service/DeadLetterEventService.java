package dev.loganalyzer.service;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.dto.DeadLetterEventResponse;
import dev.loganalyzer.dto.DeadLetterRetryResponse;
import dev.loganalyzer.dto.PagedDeadLetterEventResponse;
import dev.loganalyzer.entity.DeadLetterEvent;
import dev.loganalyzer.messaging.LogIngestionPublisher;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.messaging.DemoIngestionFailurePolicy;
import dev.loganalyzer.repository.DeadLetterEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeadLetterEventService {
    private static final int MAX_MANUAL_RETRIES = 3;

    private final DeadLetterEventRepository repository;
    private final LogIngestionPublisher publisher;
    private final DemoIngestionFailurePolicy demoFailurePolicy;

    public DeadLetterEventService(DeadLetterEventRepository repository, LogIngestionPublisher publisher,
            DemoIngestionFailurePolicy demoFailurePolicy) {
        this.repository = repository;
        this.publisher = publisher;
        this.demoFailurePolicy = demoFailurePolicy;
    }

    @Transactional
    public void record(LogRawEventV1 event, String failureReason, int retryCount, Instant failedAt) {
        if (repository.findByIngestionEventId(event.eventId()).isEmpty()) {
            repository.save(new DeadLetterEvent(event, failureReason, retryCount, failedAt));
        }
    }

    @Transactional(readOnly = true)
    public PagedDeadLetterEventResponse findAll(Pageable pageable) {
        Page<DeadLetterEventResponse> page = repository.findAll(pageable).map(this::toResponse);
        return new PagedDeadLetterEventResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements());
    }

    @Transactional
    public DeadLetterRetryResponse retry(UUID ingestionEventId) {
        DeadLetterEvent event = repository.findByIngestionEventId(ingestionEventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dead-letter event not found"));
        if (event.getManualRetryCount() >= MAX_MANUAL_RETRIES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manual retry limit reached");
        }

        publisher.republish(demoFailurePolicy.prepareManualReplay(event.getOriginalEvent()));
        Instant retriedAt = Instant.now();
        event.markRetried(retriedAt);
        return new DeadLetterRetryResponse(ingestionEventId, event.getManualRetryCount(), retriedAt, "accepted");
    }

    private DeadLetterEventResponse toResponse(DeadLetterEvent event) {
        return new DeadLetterEventResponse(event.getId(), event.getIngestionEventId(), event.getCorrelationId(),
                event.getOriginalEvent(), event.getFailureReason(), event.getRetryCount(), event.getFailedAt(),
                event.getManualRetryCount(), event.getLastRetriedAt());
    }
}