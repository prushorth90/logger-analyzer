package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.loganalyzer.dto.LogEntryResponse;
import dev.loganalyzer.dto.LogOverviewResponse;
import dev.loganalyzer.dto.LiveLogEvent;
import dev.loganalyzer.dto.LogOverviewResponse.NamedCount;
import dev.loganalyzer.dto.LogOverviewResponse.TimeCount;
import dev.loganalyzer.dto.PagedLogEntryResponse;
import dev.loganalyzer.dto.TraceResponse;
import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.messaging.LogPersistedEventV1;
import dev.loganalyzer.observability.ApplicationMetrics;
import dev.loganalyzer.repository.LogEntryRepository;
import dev.loganalyzer.repository.LogEntrySpecifications;
import dev.loganalyzer.repository.LogOverviewSummary;
import dev.loganalyzer.repository.NamedCountProjection;
import dev.loganalyzer.repository.TimeCountProjection;
import dev.loganalyzer.search.LogSearchCriteria;
import dev.loganalyzer.search.LogSearchQueryParser;
import dev.loganalyzer.search.LogSearchResult;
import dev.loganalyzer.search.OpenSearchLogIndex;
import dev.loganalyzer.search.ParsedLogSearch;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogEntryService {
    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ApplicationMetrics metrics;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OpenSearchLogIndex logIndex;
    private final LogSearchQueryParser searchQueryParser;

    public LogEntryService(LogEntryRepository logEntryRepository, ObjectMapper objectMapper, MeterRegistry meterRegistry,
            ApplicationEventPublisher applicationEventPublisher, OpenSearchLogIndex logIndex,
            LogSearchQueryParser searchQueryParser, ApplicationMetrics metrics) {
        this.logEntryRepository = logEntryRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.applicationEventPublisher = applicationEventPublisher;
        this.logIndex = logIndex;
        this.searchQueryParser = searchQueryParser;
        this.metrics = metrics;
    }

    @Transactional
    public boolean persist(LogRawEventV1 event) {
        UUID logEntryId = UUID.randomUUID();
        String metadata = serializeMetadata(event);
        Timer.Sample sample = Timer.start(meterRegistry);
        int inserted;
        try {
            inserted = logEntryRepository.insertIfAbsent(
                    logEntryId, event.eventId(), event.timestamp(), event.serviceName(), event.environment(),
                    event.severity().name(), event.message(), event.traceId(), event.host(), metadata);
        } finally {
            sample.stop(metrics.postgresqlPersistenceTimer());
        }
        if (inserted == 0) {
            metrics.duplicateEvent();
            return false;
        } else {
            applicationEventPublisher.publishEvent(LogPersistedEventV1.from(event));
            applicationEventPublisher.publishEvent(LiveLogEvent.from(logEntryId, event));
            return true;
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
    public TraceResponse findTrace(String traceId) {
        if (!StringUtils.hasText(traceId) || traceId.length() > 255) {
            throw new IllegalArgumentException("traceId must contain between 1 and 255 characters");
        }
        java.util.List<LogEntry> entries = logEntryRepository.findByTraceIdOrderByTimestampAscIdAsc(traceId);
        if (entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found");
        }

        Instant startedAt = entries.getFirst().getTimestamp();
        Instant endedAt = entries.getLast().getTimestamp();
        LinkedHashSet<String> services = new LinkedHashSet<>();
        entries.forEach(entry -> services.add(entry.getServiceName()));
        return new TraceResponse(traceId, startedAt, endedAt,
                java.time.Duration.between(startedAt, endedAt).toMillis(),
                java.util.List.copyOf(services), entries.stream().map(this::toResponse).toList());
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
        validateSort(pageable);
        long startedAt = System.nanoTime();
        if (StringUtils.hasText(search)) {
            ParsedLogSearch parsed = searchQueryParser.parse(search);
            LogSearchResult result = logIndex.search(new LogSearchCriteria(parsed.text(),
                    StringUtils.hasText(serviceName) ? serviceName : parsed.serviceName(),
                    StringUtils.hasText(environment) ? environment : parsed.environment(),
                    severity != null ? severity : parsed.severity(),
                    StringUtils.hasText(traceId) ? traceId : parsed.traceId(),
                    startTimestamp, endTimestamp), pageable);
                Map<UUID, LogEntry> entriesByEventId = logEntryRepository.findByIngestionEventIdIn(result.eventIds())
                    .stream().collect(java.util.stream.Collectors.toMap(LogEntry::getIngestionEventId, entry -> entry));
                java.util.List<LogEntryResponse> content = result.eventIds().stream()
                    .map(entriesByEventId::get)
                    .filter(java.util.Objects::nonNull)
                    .map(this::toResponse)
                    .toList();
                int totalPages = (int) Math.ceil((double) result.totalHits() / pageable.getPageSize());
                return new PagedLogEntryResponse(content, pageable.getPageNumber(), pageable.getPageSize(), totalPages,
                    result.totalHits(), elapsedMilliseconds(startedAt));
            }
        Page<LogEntryResponse> page = logEntryRepository.findAll(
                LogEntrySpecifications.withFilters(serviceName, environment, severity, traceId,
                    startTimestamp, endTimestamp, search),
                pageable)
            .map(this::toResponse);
        return new PagedLogEntryResponse(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalPages(), page.getTotalElements(), elapsedMilliseconds(startedAt));
    }

    private long elapsedMilliseconds(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted() || pageable.getSort().stream().count() != 1
                || pageable.getSort().getOrderFor("timestamp") == null) {
            throw new IllegalArgumentException("sort must be timestamp,asc or timestamp,desc");
        }
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