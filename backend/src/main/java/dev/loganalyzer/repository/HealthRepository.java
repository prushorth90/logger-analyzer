package dev.loganalyzer.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthRepository {
    private final JdbcTemplate jdbcTemplate;

    public HealthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseAvailable() {
        return Integer.valueOf(1).equals(jdbcTemplate.queryForObject("SELECT 1", Integer.class));
    }
}