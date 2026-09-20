package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

import dev.loganalyzer.entity.AlertStatus;

public record AlertResponse(
        UUID id,
        UUID ruleId,
        String ruleName,
        String serviceName,
        AlertStatus status,
        long thresholdCount,
        long observedCount,
        int windowMinutes,
        Instant windowStartedAt,
        Instant windowEndedAt,
        Instant openedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        String resolutionReason) {
}