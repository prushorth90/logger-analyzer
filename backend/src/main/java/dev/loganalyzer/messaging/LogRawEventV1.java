package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;

public record LogRawEventV1(
        int schemaVersion,
        UUID eventId,
        String correlationId,
        Instant timestamp,
        String serviceName,
        String environment,
        Severity severity,
        String message,
        String traceId,
        String host,
        Map<String, Object> metadata) {
    public static final int SCHEMA_VERSION = 1;
}