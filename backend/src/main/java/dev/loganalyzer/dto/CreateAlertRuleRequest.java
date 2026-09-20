package dev.loganalyzer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAlertRuleRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 255) String serviceName,
        @Min(1) long thresholdCount,
        @Min(1) @Max(1440) int windowMinutes,
        @Min(1) @Max(10080) int cooldownMinutes) {
}