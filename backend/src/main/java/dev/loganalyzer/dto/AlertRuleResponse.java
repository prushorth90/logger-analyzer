package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        String name,
        String serviceName,
        long thresholdCount,
        int windowMinutes,
        int cooldownMinutes,
        boolean active,
        Instant createdAt) {
}