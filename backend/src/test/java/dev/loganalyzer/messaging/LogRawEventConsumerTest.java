package dev.loganalyzer.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.observability.ApplicationMetrics;
import dev.loganalyzer.service.LogEntryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogRawEventConsumerTest {
    @Test
    void countsConsumptionAndSuccessfulEndToEndLatency() {
        LogEntryService service = mock(LogEntryService.class);
        ApplicationMetrics metrics = mock(ApplicationMetrics.class);
        LogRawEventConsumer consumer = new LogRawEventConsumer(service, metrics);
        ConsumerRecord<String, LogRawEventV1> record = record();
        when(service.persist(record.value())).thenReturn(true);

        consumer.consume(record);

        verify(metrics).rawMessageConsumed();
        verify(metrics).recordEndToEndIngestion(any());
    }

    @Test
    void countsFailedConsumerAttemptAndRethrows() {
        LogEntryService service = mock(LogEntryService.class);
        ApplicationMetrics metrics = mock(ApplicationMetrics.class);
        LogRawEventConsumer consumer = new LogRawEventConsumer(service, metrics);
        ConsumerRecord<String, LogRawEventV1> record = record();
        when(service.persist(record.value())).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> consumer.consume(record)).isInstanceOf(IllegalStateException.class);

        verify(metrics).rawMessageConsumed();
        verify(metrics).ingestionFailed();
    }

    @SuppressWarnings("unchecked")
    private ConsumerRecord<String, LogRawEventV1> record() {
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, UUID.randomUUID(), "correlation-123",
                Instant.now(), "billing-api", "production", Severity.INFO, "Accepted", null, "billing-01", Map.of());
        ConsumerRecord<String, LogRawEventV1> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(event);
        when(record.timestamp()).thenReturn(System.currentTimeMillis() - 25);
        return record;
    }
}