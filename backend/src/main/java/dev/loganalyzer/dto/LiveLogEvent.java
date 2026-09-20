package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogRawEventV1;

public record LiveLogEvent(
        UUID id,
        Instant timestamp,
        String serviceName,
        String environment,
        Severity severity,
        String message,
        String traceId) {
    public static LiveLogEvent from(UUID id, LogRawEventV1 event) {
        return new LiveLogEvent(id, event.timestamp(), event.serviceName(), event.environment(), event.severity(),
                event.message(), event.traceId());
    }
}