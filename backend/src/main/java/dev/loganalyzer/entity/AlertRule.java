package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.dto.CreateAlertRuleRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alert_rules")
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "threshold_count", nullable = false)
    private long thresholdCount;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(name = "cooldown_minutes", nullable = false)
    private int cooldownMinutes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AlertRule() {
    }

    public AlertRule(CreateAlertRuleRequest request, Instant createdAt) {
        this.name = request.name().trim();
        this.serviceName = request.serviceName().trim();
        this.thresholdCount = request.thresholdCount();
        this.windowMinutes = request.windowMinutes();
        this.cooldownMinutes = request.cooldownMinutes();
        this.active = true;
        this.createdAt = createdAt;
    }

    public void setActive(boolean active) { this.active = active; }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getServiceName() { return serviceName; }
    public long getThresholdCount() { return thresholdCount; }
    public int getWindowMinutes() { return windowMinutes; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}