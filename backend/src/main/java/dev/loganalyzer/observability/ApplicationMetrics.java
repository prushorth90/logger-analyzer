package dev.loganalyzer.observability;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMetrics {
    private final Counter logsReceived;
    private final Counter rawMessagesPublished;
    private final Counter persistedMessagesPublished;
    private final Counter rawMessagesConsumed;
    private final Counter persistedMessagesConsumed;
    private final Counter ingestionFailures;
    private final Counter dlqEvents;
    private final Counter duplicateEvents;
    private final Counter openSearchIndexingFailures;
    private final Timer postgresqlPersistence;
    private final Timer endToEndIngestion;

    public ApplicationMetrics(MeterRegistry registry) {
        logsReceived = counter(registry, "log_analyzer.ingestion.received", "Valid log requests received");
        rawMessagesPublished = kafkaCounter(registry, "log_analyzer.kafka.published", "Kafka messages published",
                "logs.raw");
        persistedMessagesPublished = kafkaCounter(registry, "log_analyzer.kafka.published",
                "Kafka messages published", "logs.persisted");
        rawMessagesConsumed = kafkaCounter(registry, "log_analyzer.kafka.consumed", "Kafka messages consumed",
                "logs.raw");
        persistedMessagesConsumed = kafkaCounter(registry, "log_analyzer.kafka.consumed",
                "Kafka messages consumed", "logs.persisted");
        ingestionFailures = counter(registry, "log_analyzer.ingestion.failures", "Failed ingestion attempts");
        dlqEvents = counter(registry, "log_analyzer.dlq.events", "Events consumed from the dead-letter topic");
        duplicateEvents = counter(registry, "log_analyzer.ingestion.duplicates", "Duplicate ingestion events");
        openSearchIndexingFailures = counter(registry, "log_analyzer.opensearch.indexing.failures",
                "Failed OpenSearch indexing attempts");
        postgresqlPersistence = timer(registry, "log_analyzer.postgresql.persistence",
                "PostgreSQL log persistence latency");
        endToEndIngestion = timer(registry, "log_analyzer.ingestion.end_to_end",
                "Kafka publication to durable PostgreSQL ingestion latency");
    }

    public void logReceived() { logsReceived.increment(); }
    public void rawMessagePublished() { rawMessagesPublished.increment(); }
    public void persistedMessagePublished() { persistedMessagesPublished.increment(); }
    public void rawMessageConsumed() { rawMessagesConsumed.increment(); }
    public void persistedMessageConsumed() { persistedMessagesConsumed.increment(); }
    public void ingestionFailed() { ingestionFailures.increment(); }
    public void dlqEventReceived() { dlqEvents.increment(); }
    public void duplicateEvent() { duplicateEvents.increment(); }
    public void openSearchIndexingFailed() { openSearchIndexingFailures.increment(); }
    public Timer postgresqlPersistenceTimer() { return postgresqlPersistence; }

    public void recordEndToEndIngestion(Duration duration) {
        endToEndIngestion.record(duration.isNegative() ? Duration.ZERO : duration);
    }

    private Counter counter(MeterRegistry registry, String name, String description) {
                return Counter.builder(name).description(description).register(registry);
    }

    private Counter kafkaCounter(MeterRegistry registry, String name, String description, String topic) {
                return Counter.builder(name).description(description).tag("topic", topic)
                .register(registry);
    }

    private Timer timer(MeterRegistry registry, String name, String description) {
        return Timer.builder(name).description(description).publishPercentileHistogram().register(registry);
    }
}