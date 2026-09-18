package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Map;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogEntryResponse;
import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogEntryServiceTest {
    @Test
    void createsAndMapsLogEntry() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        when(repository.save(any(LogEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LogEntryService service = new LogEntryService(repository);
        Instant timestamp = Instant.parse("2026-09-17T12:00:00Z");
        Map<String, Object> metadata = Map.of("requestMethod", "POST", "durationMs", 42);
        CreateLogEntryRequest request = new CreateLogEntryRequest(timestamp, "billing-api", "production",
                Severity.ERROR, "Payment failed", "trace-123", "billing-01", metadata);

        LogEntryResponse response = service.create(request);

        ArgumentCaptor<LogEntry> captor = ArgumentCaptor.forClass(LogEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getServiceName()).isEqualTo("billing-api");
        assertThat(response.timestamp()).isEqualTo(timestamp);
        assertThat(response.environment()).isEqualTo("production");
        assertThat(response.severity()).isEqualTo(Severity.ERROR);
        assertThat(response.message()).isEqualTo("Payment failed");
        assertThat(response.traceId()).isEqualTo("trace-123");
        assertThat(response.host()).isEqualTo("billing-01");
        assertThat(response.metadata()).isEqualTo(metadata);
    }
}