package dev.loganalyzer.messaging;

import dev.loganalyzer.observability.ApplicationMetrics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LogPersistedEventPublisher {
    public static final String TOPIC = "logs.persisted";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationMetrics metrics;

    public LogPersistedEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(LogPersistedEventV1 event) {
        kafkaTemplate.send(TOPIC, event.eventId().toString(), event).whenComplete((result, exception) -> {
            if (exception == null) {
                metrics.persistedMessagePublished();
            } else {
                metrics.ingestionFailed();
            }
        });
    }
}