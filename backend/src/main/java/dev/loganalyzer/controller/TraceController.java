package dev.loganalyzer.controller;

import dev.loganalyzer.dto.TraceResponse;
import dev.loganalyzer.service.LogEntryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
public class TraceController {
    private final LogEntryService logEntryService;

    public TraceController(LogEntryService logEntryService) {
        this.logEntryService = logEntryService;
    }

    @GetMapping("/{traceId}")
    public TraceResponse findByTraceId(@PathVariable String traceId) {
        return logEntryService.findTrace(traceId);
    }
}