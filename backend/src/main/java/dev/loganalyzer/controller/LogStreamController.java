package dev.loganalyzer.controller;

import dev.loganalyzer.service.LogStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/logs")
public class LogStreamController {
    private final LogStreamService logStreamService;

    public LogStreamController(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return logStreamService.subscribe();
    }
}