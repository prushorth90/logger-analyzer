package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.Map;

import dev.loganalyzer.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLogEntryRequest(
        @NotNull Instant timestamp,
        @NotBlank @Size(max = 255) String serviceName,
        @NotBlank @Size(max = 64) String environment,
        @NotNull Severity severity,
        @NotBlank String message,
        @Size(max = 255) String traceId,
        @NotBlank @Size(max = 255) String host,
        Map<String, Object> metadata) {
}