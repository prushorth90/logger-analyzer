package dev.loganalyzer.controller;

import java.net.URI;

import dev.loganalyzer.dto.CreateLogEntryRequest;
import dev.loganalyzer.dto.LogEntryResponse;
import dev.loganalyzer.service.LogEntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogEntryController {
    private final LogEntryService logEntryService;

    public LogEntryController(LogEntryService logEntryService) {
        this.logEntryService = logEntryService;
    }

    @PostMapping
    public ResponseEntity<LogEntryResponse> create(@Valid @RequestBody CreateLogEntryRequest request) {
        LogEntryResponse response = logEntryService.create(request);
        return ResponseEntity.created(URI.create("/api/logs/" + response.id())).body(response);
    }
}