package dev.loganalyzer.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LogPersistedEventPublisher {
    public static final String TOPIC = "logs.persisted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public LogPersistedEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(LogPersistedEventV1 event) {
        kafkaTemplate.send(TOPIC, event.eventId().toString(), event);
    }
}