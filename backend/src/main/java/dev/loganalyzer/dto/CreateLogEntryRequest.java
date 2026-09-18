package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.Map;

import dev.loganalyzer.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLogEntryRequest(
        @NotNull Instant timestamp,
        @NotBlank String serviceName,
        @NotBlank String environment,
        @NotNull Severity severity,
        @NotBlank String message,
        String traceId,
        @NotBlank String host,
        Map<String, Object> metadata) {
}