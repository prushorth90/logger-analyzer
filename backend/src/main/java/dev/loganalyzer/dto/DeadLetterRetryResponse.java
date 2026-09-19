package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterRetryResponse(UUID ingestionEventId, int manualRetryCount, Instant retriedAt, String status) {
}