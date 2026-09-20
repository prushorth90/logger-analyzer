package dev.loganalyzer.messaging;

import java.util.UUID;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogIngestionAcceptedResponse;
import dev.loganalyzer.observability.ApplicationMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LogIngestionPublisher {
    public static final String TOPIC = "logs.raw";
    public static final String DEAD_LETTER_TOPIC = "logs.raw.dlq";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogIngestionPublisher.class);

    private final KafkaTemplate<String, LogRawEventV1> kafkaTemplate;
    private final ApplicationMetrics metrics;

    public LogIngestionPublisher(KafkaTemplate<String, LogRawEventV1> kafkaTemplate, ApplicationMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    public LogIngestionAcceptedResponse publish(CreateLogEntryRequest request, String suppliedCorrelationId) {
        metrics.logReceived();
        UUID eventId = UUID.randomUUID();
        String correlationId = StringUtils.hasText(suppliedCorrelationId)
                ? suppliedCorrelationId
                : UUID.randomUUID().toString();
        LogRawEventV1 event = new LogRawEventV1(
                LogRawEventV1.SCHEMA_VERSION,
                eventId,
                correlationId,
                request.timestamp(),
                request.serviceName(),
                request.environment(),
                request.severity(),
                request.message(),
                request.traceId(),
                request.host(),
                request.metadata());

        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            LOGGER.info("Publishing log event eventId={} topic={}", eventId, TOPIC);
            kafkaTemplate.send(TOPIC, eventId.toString(), event).whenComplete((result, exception) -> {
                try (MDC.MDCCloseable callbackMdc = MDC.putCloseable("correlationId", correlationId)) {
                    if (exception == null) {
                        metrics.rawMessagePublished();
                        LOGGER.info("Published log event eventId={} partition={} offset={}", eventId,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        metrics.ingestionFailed();
                        LOGGER.error("Failed to publish log event eventId={} topic={}", eventId, TOPIC, exception);
                    }
                }
            });
        }

        return new LogIngestionAcceptedResponse(eventId, correlationId, "accepted");
    }

    public void republish(LogRawEventV1 event) {
        try {
            kafkaTemplate.send(TOPIC, event.eventId().toString(), event).join();
            metrics.rawMessagePublished();
        } catch (RuntimeException exception) {
            metrics.ingestionFailed();
            throw exception;
        }
    }
}