package dev.loganalyzer.messaging;

import java.time.Duration;
import java.time.Instant;

import dev.loganalyzer.observability.ApplicationMetrics;
import dev.loganalyzer.service.LogEntryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogRawEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogRawEventConsumer.class);

    private final LogEntryService logEntryService;
    private final ApplicationMetrics metrics;

    public LogRawEventConsumer(LogEntryService logEntryService, ApplicationMetrics metrics) {
        this.logEntryService = logEntryService;
        this.metrics = metrics;
    }

    @KafkaListener(topics = LogIngestionPublisher.TOPIC)
    public void consume(ConsumerRecord<String, LogRawEventV1> record) {
        LogRawEventV1 event = record.value();
        metrics.rawMessageConsumed();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", event.correlationId())) {
            try {
                if (event.eventId() == null) {
                    throw new IllegalArgumentException("logs.raw eventId is required");
                }
                if (event.schemaVersion() != LogRawEventV1.SCHEMA_VERSION) {
                    throw new IllegalArgumentException("Unsupported logs.raw schema version: " + event.schemaVersion());
                }
                LOGGER.info("Consuming log event eventId={} schemaVersion={}", event.eventId(), event.schemaVersion());
                if (logEntryService.persist(event)) {
                    metrics.recordEndToEndIngestion(Duration.between(Instant.ofEpochMilli(record.timestamp()),
                            Instant.now()));
                }
                LOGGER.info("Persisted log event eventId={}", event.eventId());
            } catch (RuntimeException exception) {
                metrics.ingestionFailed();
                throw exception;
            }
        }
    }
}