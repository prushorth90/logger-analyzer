package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule rule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AlertStatus status;

    @Column(name = "observed_count", nullable = false)
    private long observedCount;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "window_ended_at", nullable = false)
    private Instant windowEndedAt;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_reason")
    private String resolutionReason;

    protected Alert() {
    }

    public Alert(AlertRule rule, long observedCount, Instant windowStartedAt, Instant windowEndedAt) {
        this.rule = rule;
        this.status = AlertStatus.OPEN;
        this.observedCount = observedCount;
        this.windowStartedAt = windowStartedAt;
        this.windowEndedAt = windowEndedAt;
        this.openedAt = windowEndedAt;
    }

    public void acknowledge(Instant at) {
        if (status != AlertStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN alerts can be acknowledged");
        }
        status = AlertStatus.ACKNOWLEDGED;
        acknowledgedAt = at;
    }

    public void resolve(Instant at, String reason) {
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalArgumentException("Alert is already resolved");
        }
        status = AlertStatus.RESOLVED;
        resolvedAt = at;
        resolutionReason = reason;
    }

    public UUID getId() { return id; }
    public AlertRule getRule() { return rule; }
    public AlertStatus getStatus() { return status; }
    public long getObservedCount() { return observedCount; }
    public Instant getWindowStartedAt() { return windowStartedAt; }
    public Instant getWindowEndedAt() { return windowEndedAt; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionReason() { return resolutionReason; }
}