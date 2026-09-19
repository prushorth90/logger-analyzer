package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;

public record LogPersistedEventV1(
        int schemaVersion,
        UUID eventId,
        Instant timestamp,
        String serviceName,
        String environment,
        Severity severity,
        String message,
        String traceId) {
    public static final int SCHEMA_VERSION = 1;

    public static LogPersistedEventV1 from(LogRawEventV1 event) {
        return new LogPersistedEventV1(SCHEMA_VERSION, event.eventId(), event.timestamp(), event.serviceName(),
                event.environment(), event.severity(), event.message(), event.traceId());
    }
}