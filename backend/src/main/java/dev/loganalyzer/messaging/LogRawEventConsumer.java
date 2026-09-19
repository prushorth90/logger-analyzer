package dev.loganalyzer.messaging;

import dev.loganalyzer.service.LogEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogRawEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogRawEventConsumer.class);

    private final LogEntryService logEntryService;

    public LogRawEventConsumer(LogEntryService logEntryService) {
        this.logEntryService = logEntryService;
    }

    @KafkaListener(topics = LogIngestionPublisher.TOPIC)
    public void consume(LogRawEventV1 event) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", event.correlationId())) {
            if (event.eventId() == null) {
                throw new IllegalArgumentException("logs.raw eventId is required");
            }
            if (event.schemaVersion() != LogRawEventV1.SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported logs.raw schema version: " + event.schemaVersion());
            }
            LOGGER.info("Consuming log event eventId={} schemaVersion={}", event.eventId(), event.schemaVersion());
            logEntryService.persist(event);
            LOGGER.info("Persisted log event eventId={}", event.eventId());
        }
    }
}