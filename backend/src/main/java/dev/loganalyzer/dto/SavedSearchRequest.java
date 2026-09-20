package dev.loganalyzer.dto;

import java.time.Instant;

import dev.loganalyzer.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavedSearchRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String query,
        Severity severity,
        @Size(max = 255) String serviceName,
        @Size(max = 64) String environment,
        @Size(max = 255) String traceId,
        Instant startTimestamp,
        Instant endTimestamp,
        @NotNull SearchSortDirection sortDirection) {
}