package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.dto.SavedSearchRequest;
import dev.loganalyzer.dto.SearchSortDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "saved_searches")
public class SavedSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 500)
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Severity severity;

    @Column(name = "service_name")
    private String serviceName;

    @Column(length = 64)
    private String environment;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "start_timestamp")
    private Instant startTimestamp;

    @Column(name = "end_timestamp")
    private Instant endTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "sort_direction", nullable = false, length = 16)
    private SearchSortDirection sortDirection;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedSearch() {
    }

    public SavedSearch(SavedSearchRequest request, Instant createdAt) {
        this.name = request.name().trim();
        this.query = request.query();
        this.severity = request.severity();
        this.serviceName = request.serviceName();
        this.environment = request.environment();
        this.traceId = request.traceId();
        this.startTimestamp = request.startTimestamp();
        this.endTimestamp = request.endTimestamp();
        this.sortDirection = request.sortDirection();
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getQuery() { return query; }
    public Severity getSeverity() { return severity; }
    public String getServiceName() { return serviceName; }
    public String getEnvironment() { return environment; }
    public String getTraceId() { return traceId; }
    public Instant getStartTimestamp() { return startTimestamp; }
    public Instant getEndTimestamp() { return endTimestamp; }
    public SearchSortDirection getSortDirection() { return sortDirection; }
    public Instant getCreatedAt() { return createdAt; }
}