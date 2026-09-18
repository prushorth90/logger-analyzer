package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;

public record LogEntryResponse(
        UUID id,
        Instant timestamp,
        String serviceName,
        String environment,
        Severity severity,
        String message,
        String traceId,
        String host,
        Map<String, Object> metadata) {
}