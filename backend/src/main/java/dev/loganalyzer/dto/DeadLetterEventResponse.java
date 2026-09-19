package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.messaging.LogRawEventV1;

public record DeadLetterEventResponse(
        UUID id,
        UUID ingestionEventId,
        String correlationId,
        LogRawEventV1 originalEvent,
        String failureReason,
        int retryCount,
        Instant failedAt,
        int manualRetryCount,
        Instant lastRetriedAt) {
}