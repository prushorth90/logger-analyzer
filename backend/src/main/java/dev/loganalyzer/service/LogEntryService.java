package dev.loganalyzer.service;

import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogEntryResponse;
import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.repository.LogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogEntryService {
    private final LogEntryRepository logEntryRepository;

    public LogEntryService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    @Transactional
    public LogEntryResponse create(CreateLogEntryRequest request) {
        LogEntry logEntry = new LogEntry(request.timestamp(), request.serviceName(), request.environment(),
                request.severity(), request.message(), request.traceId(), request.host(), request.metadata());
        return toResponse(logEntryRepository.save(logEntry));
    }

    @Transactional(readOnly = true)
    public Optional<LogEntryResponse> findById(UUID id) {
        return logEntryRepository.findById(id).map(this::toResponse);
    }

    private LogEntryResponse toResponse(LogEntry logEntry) {
        return new LogEntryResponse(logEntry.getId(), logEntry.getTimestamp(), logEntry.getServiceName(),
                logEntry.getEnvironment(), logEntry.getSeverity(), logEntry.getMessage(), logEntry.getTraceId(),
                logEntry.getHost(), logEntry.getMetadata());
    }
}