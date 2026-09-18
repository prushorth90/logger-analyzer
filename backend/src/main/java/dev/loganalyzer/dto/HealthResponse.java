package dev.loganalyzer.dto;

import java.time.Instant;

public record HealthResponse(String status, String service, String database, Instant timestamp) {
}