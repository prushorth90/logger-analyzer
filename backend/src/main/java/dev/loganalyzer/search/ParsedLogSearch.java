package dev.loganalyzer.search;

import dev.loganalyzer.entity.Severity;

public record ParsedLogSearch(
        String text,
        String serviceName,
        String environment,
        Severity severity,
        String traceId) {
}