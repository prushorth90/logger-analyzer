package dev.loganalyzer.messaging;

import dev.loganalyzer.search.OpenSearchLogIndex;
import dev.loganalyzer.observability.ApplicationMetrics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogPersistedEventConsumer {
    private final OpenSearchLogIndex logIndex;
    private final ApplicationMetrics metrics;

    public LogPersistedEventConsumer(OpenSearchLogIndex logIndex, ApplicationMetrics metrics) {
        this.logIndex = logIndex;
        this.metrics = metrics;
    }

    @KafkaListener(
            topics = LogPersistedEventPublisher.TOPIC,
            groupId = "log-analyzer-opensearch-index-v1",
            autoStartup = "${log-analyzer.opensearch.indexing-enabled:true}",
            containerFactory = "indexingKafkaListenerContainerFactory")
    public void consume(LogPersistedEventV1 event) {
        metrics.persistedMessageConsumed();
        if (event.schemaVersion() != LogPersistedEventV1.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported logs.persisted schema version: " + event.schemaVersion());
        }
        logIndex.index(event);
    }
}