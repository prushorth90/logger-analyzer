package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.List;

public record TraceResponse(
        String traceId,
        Instant startedAt,
        Instant endedAt,
        long durationMs,
        List<String> serviceSequence,
        List<LogEntryResponse> events) {
}