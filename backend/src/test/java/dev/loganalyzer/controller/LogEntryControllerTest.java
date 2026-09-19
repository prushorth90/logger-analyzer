package dev.loganalyzer.controller;

import dev.loganalyzer.messaging.LogIngestionPublisher;
import dev.loganalyzer.service.LogEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogEntryControllerTest {
    private LogEntryService service;
    private LogIngestionPublisher publisher;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(LogEntryService.class);
        publisher = mock(LogIngestionPublisher.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LogEntryController(service, publisher))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsGenericResponseForUnexpectedErrors() throws Exception {
        when(publisher.publish(any(), any())).thenThrow(new IllegalStateException("private Kafka details"));

        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timestamp": "2026-09-17T12:00:00Z",
                                  "serviceName": "billing-api",
                                  "environment": "production",
                                  "severity": "ERROR",
                                  "message": "Payment failed",
                                  "host": "billing-01"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/logs"))
                .andExpect(jsonPath("$.*", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("private database details"))));
    }
}