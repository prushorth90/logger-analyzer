package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "log_entries")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ingestion_event_id", unique = true)
    private UUID ingestionEventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Column(nullable = false, length = 64)
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "trace_id", length = 255)
    private String traceId;

    @Column(nullable = false, length = 255)
    private String host;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected LogEntry() {
    }

    public LogEntry(Instant timestamp, String serviceName, String environment, Severity severity,
            String message, String traceId, String host, Map<String, Object> metadata) {
        this(null, timestamp, serviceName, environment, severity, message, traceId, host, metadata);
    }

    public LogEntry(UUID ingestionEventId, Instant timestamp, String serviceName, String environment, Severity severity,
            String message, String traceId, String host, Map<String, Object> metadata) {
        this.ingestionEventId = ingestionEventId;
        this.timestamp = timestamp;
        this.serviceName = serviceName;
        this.environment = environment;
        this.severity = severity;
        this.message = message;
        this.traceId = traceId;
        this.host = host;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public UUID getIngestionEventId() { return ingestionEventId; }
    public Instant getTimestamp() { return timestamp; }
    public String getServiceName() { return serviceName; }
    public String getEnvironment() { return environment; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getTraceId() { return traceId; }
    public String getHost() { return host; }
    public Map<String, Object> getMetadata() { return metadata; }
}