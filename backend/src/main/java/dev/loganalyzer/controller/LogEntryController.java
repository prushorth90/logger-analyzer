package dev.loganalyzer.controller;

import java.time.Instant;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogIngestionAcceptedResponse;
import dev.loganalyzer.dto.LogOverviewResponse;
import dev.loganalyzer.dto.PagedLogEntryResponse;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogIngestionPublisher;
import dev.loganalyzer.service.LogEntryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogEntryController {
    private final LogEntryService logEntryService;
    private final LogIngestionPublisher logIngestionPublisher;

    public LogEntryController(LogEntryService logEntryService, LogIngestionPublisher logIngestionPublisher) {
        this.logEntryService = logEntryService;
        this.logIngestionPublisher = logIngestionPublisher;
    }

    @GetMapping
    public PagedLogEntryResponse findAll(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Instant startTimestamp,
            @RequestParam(required = false) Instant endTimestamp,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return logEntryService.findAll(serviceName, environment, severity, traceId, startTimestamp, endTimestamp,
                search, pageable);
    }

    @GetMapping("/overview")
    public LogOverviewResponse getOverview(
            @RequestParam Instant startTimestamp,
            @RequestParam Instant endTimestamp) {
        return logEntryService.getOverview(startTimestamp, endTimestamp);
    }

    @PostMapping
    public ResponseEntity<LogIngestionAcceptedResponse> create(
            @Valid @RequestBody CreateLogEntryRequest request,
            @RequestHeader(name = LogIngestionPublisher.CORRELATION_ID_HEADER, required = false)
            String correlationId) {
        LogIngestionAcceptedResponse response = logIngestionPublisher.publish(request, correlationId);
        return ResponseEntity.accepted()
                .header(LogIngestionPublisher.CORRELATION_ID_HEADER, response.correlationId())
                .body(response);
    }
}