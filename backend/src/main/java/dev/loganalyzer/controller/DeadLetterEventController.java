package dev.loganalyzer.controller;

import java.util.UUID;

import dev.loganalyzer.dto.DeadLetterRetryResponse;
import dev.loganalyzer.dto.PagedDeadLetterEventResponse;
import dev.loganalyzer.service.DeadLetterEventService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dead-letter-events")
public class DeadLetterEventController {
    private final DeadLetterEventService service;

    public DeadLetterEventController(DeadLetterEventService service) {
        this.service = service;
    }

    @GetMapping
    public PagedDeadLetterEventResponse findAll(
            @PageableDefault(size = 20, sort = "failedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.findAll(pageable);
    }

    @PostMapping("/{ingestionEventId}/retry")
    public ResponseEntity<DeadLetterRetryResponse> retry(@PathVariable UUID ingestionEventId) {
        return ResponseEntity.accepted().body(service.retry(ingestionEventId));
    }
}