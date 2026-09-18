package dev.loganalyzer.dto;

import java.time.Instant;
import java.util.List;

public record LogOverviewResponse(
        Instant startTimestamp,
        Instant endTimestamp,
        long totalLogs,
        long errorCount,
        long warningCount,
        long activeServices,
        List<NamedCount> logsByService,
        List<NamedCount> logsBySeverity,
        List<TimeCount> logsOverTime,
        List<NamedCount> errorsByService) {

    public record NamedCount(String name, long count) {
    }

    public record TimeCount(Instant timestamp, long count) {
    }
}