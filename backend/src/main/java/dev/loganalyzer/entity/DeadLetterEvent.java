package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.messaging.LogRawEventV1;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ingestion_event_id", nullable = false, unique = true)
    private UUID ingestionEventId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_event", nullable = false, columnDefinition = "jsonb")
    private LogRawEventV1 originalEvent;

    @Column(name = "failure_reason", nullable = false, columnDefinition = "text")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    @Column(name = "manual_retry_count", nullable = false)
    private int manualRetryCount;

    @Column(name = "last_retried_at")
    private Instant lastRetriedAt;

    protected DeadLetterEvent() {
    }

    public DeadLetterEvent(LogRawEventV1 originalEvent, String failureReason, int retryCount, Instant failedAt) {
        this.ingestionEventId = originalEvent.eventId();
        this.correlationId = originalEvent.correlationId();
        this.originalEvent = originalEvent;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.failedAt = failedAt;
    }

    public void markRetried(Instant retriedAt) {
        manualRetryCount++;
        lastRetriedAt = retriedAt;
    }

    public UUID getId() { return id; }
    public UUID getIngestionEventId() { return ingestionEventId; }
    public String getCorrelationId() { return correlationId; }
    public LogRawEventV1 getOriginalEvent() { return originalEvent; }
    public String getFailureReason() { return failureReason; }
    public int getRetryCount() { return retryCount; }
    public Instant getFailedAt() { return failedAt; }
    public int getManualRetryCount() { return manualRetryCount; }
    public Instant getLastRetriedAt() { return lastRetriedAt; }
}