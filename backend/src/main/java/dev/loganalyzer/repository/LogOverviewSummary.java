package dev.loganalyzer.repository;

public interface LogOverviewSummary {
    long getTotalLogs();

    long getErrorCount();

    long getWarningCount();

    long getActiveServices();
}