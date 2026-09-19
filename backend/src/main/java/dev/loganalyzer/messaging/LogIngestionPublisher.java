package dev.loganalyzer.messaging;

import java.util.UUID;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogIngestionAcceptedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LogIngestionPublisher {
    public static final String TOPIC = "logs.raw";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogIngestionPublisher.class);

    private final KafkaTemplate<String, LogRawEventV1> kafkaTemplate;

    public LogIngestionPublisher(KafkaTemplate<String, LogRawEventV1> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public LogIngestionAcceptedResponse publish(CreateLogEntryRequest request, String suppliedCorrelationId) {
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
                        LOGGER.info("Published log event eventId={} partition={} offset={}", eventId,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        LOGGER.error("Failed to publish log event eventId={} topic={}", eventId, TOPIC, exception);
                    }
                }
            });
        }

        return new LogIngestionAcceptedResponse(eventId, correlationId, "accepted");
    }
}