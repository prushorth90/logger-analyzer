package dev.loganalyzer.search;

import java.time.Instant;

import dev.loganalyzer.entity.Severity;

public record LogSearchCriteria(
        String text,
        String serviceName,
        String environment,
        Severity severity,
        String traceId,
        Instant startTimestamp,
        Instant endTimestamp) {
}