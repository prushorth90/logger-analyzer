package dev.loganalyzer.controller;

import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class LogEntryControllerIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogEntryRepository repository;

    @BeforeEach
    void clearLogs() {
        repository.deleteAll();
    }

    @Test
    void createsAndPersistsLogEntry() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timestamp": "2026-09-17T12:00:00Z",
                                  "serviceName": "billing-api",
                                  "environment": "production",
                                  "severity": "ERROR",
                                  "message": "Payment failed",
                                  "traceId": "trace-123",
                                  "host": "billing-01",
                                  "metadata": {"durationMs": 42}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/logs/[0-9a-f-]+")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.severity").value("ERROR"))
                .andExpect(jsonPath("$.metadata.durationMs").value(42));

        assertThat(repository.findAll()).singleElement().satisfies(logEntry -> {
            assertThat(logEntry.getServiceName()).isEqualTo("billing-api");
            assertThat(logEntry.getSeverity()).isEqualTo(Severity.ERROR);
            assertThat(logEntry.getMessage()).isEqualTo("Payment failed");
        });
    }

    @Test
    void rejectsInvalidLogEntry() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceName": " ",
                                  "environment": "production",
                                  "severity": "INFO",
                                  "message": "",
                                  "host": "api-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/logs"))
                .andExpect(jsonPath("$.fieldErrors.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors.serviceName").exists())
                .andExpect(jsonPath("$.fieldErrors.message").exists());

        assertThat(repository.count()).isZero();
    }

    @Test
    void rejectsMalformedJsonWithoutLeakingDetails() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"CRITICAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}