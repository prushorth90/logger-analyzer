package dev.loganalyzer.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("log-analyzer.ingestion.retry")
public record LogIngestionRetryProperties(int maxAttempts, Duration interval) {
    public LogIngestionRetryProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
    }
}