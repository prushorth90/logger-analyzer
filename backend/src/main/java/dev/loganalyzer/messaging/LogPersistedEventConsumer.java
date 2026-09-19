package dev.loganalyzer.messaging;

import dev.loganalyzer.search.OpenSearchLogIndex;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogPersistedEventConsumer {
    private final OpenSearchLogIndex logIndex;

    public LogPersistedEventConsumer(OpenSearchLogIndex logIndex) {
        this.logIndex = logIndex;
    }

    @KafkaListener(
            topics = LogPersistedEventPublisher.TOPIC,
            groupId = "log-analyzer-opensearch-index-v1",
            autoStartup = "${log-analyzer.opensearch.indexing-enabled:true}",
            containerFactory = "indexingKafkaListenerContainerFactory")
    public void consume(LogPersistedEventV1 event) {
        if (event.schemaVersion() != LogPersistedEventV1.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported logs.persisted schema version: " + event.schemaVersion());
        }
        logIndex.index(event);
    }
}