package dev.loganalyzer.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import dev.loganalyzer.service.DeadLetterEventService;
import dev.loganalyzer.observability.ApplicationMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterEventConsumer {
    private final DeadLetterEventService service;
    private final LogIngestionRetryProperties retryProperties;
    private final ApplicationMetrics metrics;

    public DeadLetterEventConsumer(DeadLetterEventService service, LogIngestionRetryProperties retryProperties,
            ApplicationMetrics metrics) {
        this.service = service;
        this.retryProperties = retryProperties;
        this.metrics = metrics;
    }

        @KafkaListener(
            topics = LogIngestionPublisher.DEAD_LETTER_TOPIC,
            groupId = "log-analyzer-dlq-projection-v1",
            containerFactory = "deadLetterKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, LogRawEventV1> record) {
        metrics.dlqEventReceived();
        String reason = headerValue(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE, "Unknown ingestion failure");
        String exceptionType = headerValue(record, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN,
            headerValue(record, KafkaHeaders.DLT_EXCEPTION_FQCN, ""));
        int retryCount = IllegalArgumentException.class.getName().equals(exceptionType)
                ? 0
                : retryProperties.maxAttempts() - 1;
        service.record(record.value(), reason, retryCount, Instant.now());
    }

    private String headerValue(ConsumerRecord<?, ?> record, String name, String fallback) {
        Header header = record.headers().lastHeader(name);
        return header == null ? fallback : new String(header.value(), StandardCharsets.UTF_8);
    }
}