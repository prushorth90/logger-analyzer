package dev.loganalyzer.observability;

import java.time.Duration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationMetricsTest {
    @Test
    void registersLowCardinalityCountersAndTimers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApplicationMetrics metrics = new ApplicationMetrics(registry);

        metrics.logReceived();
        metrics.rawMessagePublished();
        metrics.persistedMessagePublished();
        metrics.rawMessageConsumed();
        metrics.persistedMessageConsumed();
        metrics.ingestionFailed();
        metrics.dlqEventReceived();
        metrics.duplicateEvent();
        metrics.openSearchIndexingFailed();
        metrics.postgresqlPersistenceTimer().record(Duration.ofMillis(12));
        metrics.recordEndToEndIngestion(Duration.ofMillis(25));

        assertThat(registry.counter("log_analyzer.ingestion.received").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.kafka.published", "topic", "logs.raw").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.kafka.published", "topic", "logs.persisted").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.kafka.consumed", "topic", "logs.raw").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.kafka.consumed", "topic", "logs.persisted").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.ingestion.failures").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.dlq.events").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.ingestion.duplicates").count()).isEqualTo(1);
        assertThat(registry.counter("log_analyzer.opensearch.indexing.failures").count()).isEqualTo(1);
        assertThat(registry.timer("log_analyzer.postgresql.persistence").count()).isEqualTo(1);
        assertThat(registry.timer("log_analyzer.ingestion.end_to_end").count()).isEqualTo(1);
    }
}