package dev.loganalyzer.service;

import java.time.Instant;

import dev.loganalyzer.dto.HealthResponse;
import dev.loganalyzer.repository.HealthRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final HealthRepository healthRepository;

    public HealthService(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    public HealthResponse checkHealth() {
        boolean available;
        try {
            available = healthRepository.isDatabaseAvailable();
        } catch (DataAccessException exception) {
            available = false;
        }
        return new HealthResponse(available ? "UP" : "DOWN", "log-analyzer",
                available ? "UP" : "DOWN", Instant.now());
    }
}