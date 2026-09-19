package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.loganalyzer.dto.LogEntryResponse;
import dev.loganalyzer.dto.LogOverviewResponse;
import dev.loganalyzer.dto.LogOverviewResponse.NamedCount;
import dev.loganalyzer.dto.LogOverviewResponse.TimeCount;
import dev.loganalyzer.dto.PagedLogEntryResponse;
import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.repository.LogEntryRepository;
import dev.loganalyzer.repository.LogEntrySpecifications;
import dev.loganalyzer.repository.LogOverviewSummary;
import dev.loganalyzer.repository.NamedCountProjection;
import dev.loganalyzer.repository.TimeCountProjection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogEntryService {
    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;
    private final Counter duplicateEvents;

    public LogEntryService(LogEntryRepository logEntryRepository, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.logEntryRepository = logEntryRepository;
        this.objectMapper = objectMapper;
        this.duplicateEvents = Counter.builder("log.ingestion.duplicates")
                .description("Kafka log events ignored because their event ID was already persisted")
                .register(meterRegistry);
    }

    @Transactional
    public void persist(LogRawEventV1 event) {
        int inserted = logEntryRepository.insertIfAbsent(
                UUID.randomUUID(), event.eventId(), event.timestamp(), event.serviceName(), event.environment(),
                event.severity().name(), event.message(), event.traceId(), event.host(), serializeMetadata(event));
        if (inserted == 0) {
            duplicateEvents.increment();
        }
    }

    private String serializeMetadata(LogRawEventV1 event) {
        if (event.metadata() == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(event.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Log event metadata is not valid JSON", exception);
        }
    }

    @Transactional(readOnly = true)
    public Optional<LogEntryResponse> findById(UUID id) {
        return logEntryRepository.findById(id).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PagedLogEntryResponse findAll(
            String serviceName,
            String environment,
            Severity severity,
            String traceId,
            Instant startTimestamp,
            Instant endTimestamp,
            String search,
            Pageable pageable) {
        Page<LogEntryResponse> page = logEntryRepository.findAll(
                LogEntrySpecifications.withFilters(serviceName, environment, severity, traceId,
                    startTimestamp, endTimestamp, search),
                pageable)
            .map(this::toResponse);
        return new PagedLogEntryResponse(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalPages(), page.getTotalElements());
    }

        @Cacheable(cacheNames = "log-overview",
            key = "#startTimestamp.toEpochMilli() + ':' + #endTimestamp.toEpochMilli()",
            sync = true)
    @Transactional(readOnly = true)
    public LogOverviewResponse getOverview(Instant startTimestamp, Instant endTimestamp) {
        if (!startTimestamp.isBefore(endTimestamp)) {
            throw new IllegalArgumentException("startTimestamp must be before endTimestamp");
        }

        LogOverviewSummary summary = logEntryRepository.summarize(startTimestamp, endTimestamp);
        return new LogOverviewResponse(
                startTimestamp,
                endTimestamp,
                summary.getTotalLogs(),
                summary.getErrorCount(),
                summary.getWarningCount(),
                summary.getActiveServices(),
                logEntryRepository.countByService(startTimestamp, endTimestamp).stream()
                        .map(this::toNamedCount).toList(),
                logEntryRepository.countBySeverity(startTimestamp, endTimestamp).stream()
                        .map(this::toNamedCount).toList(),
                logEntryRepository.countByHour(startTimestamp, endTimestamp).stream()
                        .map(this::toTimeCount).toList(),
                logEntryRepository.countErrorsByService(startTimestamp, endTimestamp).stream()
                        .map(this::toNamedCount).toList());
    }

    private NamedCount toNamedCount(NamedCountProjection projection) {
        return new NamedCount(projection.getName(), projection.getCount());
    }

    private TimeCount toTimeCount(TimeCountProjection projection) {
        return new TimeCount(projection.getTimestamp(), projection.getCount());
    }

    private LogEntryResponse toResponse(LogEntry logEntry) {
        return new LogEntryResponse(logEntry.getId(), logEntry.getTimestamp(), logEntry.getServiceName(),
                logEntry.getEnvironment(), logEntry.getSeverity(), logEntry.getMessage(), logEntry.getTraceId(),
                logEntry.getHost(), logEntry.getMetadata());
    }
}