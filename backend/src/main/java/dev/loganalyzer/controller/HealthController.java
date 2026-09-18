package dev.loganalyzer.controller;

import dev.loganalyzer.dto.HealthResponse;
import dev.loganalyzer.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.checkHealth();
        return ResponseEntity.status("UP".equals(response.status())
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}