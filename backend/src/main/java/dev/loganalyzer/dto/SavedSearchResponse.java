package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;

public record SavedSearchResponse(
        UUID id,
        String name,
        String query,
        Severity severity,
        String serviceName,
        String environment,
        String traceId,
        Instant startTimestamp,
        Instant endTimestamp,
        SearchSortDirection sortDirection,
        Instant createdAt) {
}