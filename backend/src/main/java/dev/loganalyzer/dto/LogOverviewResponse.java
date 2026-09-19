package dev.loganalyzer.dto;

import java.io.Serializable;
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
        List<NamedCount> errorsByService) implements Serializable {

    public record NamedCount(String name, long count) implements Serializable {
    }

    public record TimeCount(Instant timestamp, long count) implements Serializable {
    }
}