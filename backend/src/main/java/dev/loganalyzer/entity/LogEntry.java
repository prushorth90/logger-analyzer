package dev.loganalyzer.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "log_entries")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    protected LogEntry() {
    }

    public LogEntry(Instant timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public UUID getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getMessage() { return message; }
}