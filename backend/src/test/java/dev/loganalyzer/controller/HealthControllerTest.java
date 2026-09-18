package dev.loganalyzer.controller;

import dev.loganalyzer.repository.HealthRepository;
import dev.loganalyzer.service.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {
    private HealthRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = mock(HealthRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new HealthController(new HealthService(repository))).build();
    }

    @Test
    void reportsHealthyDatabase() throws Exception {
        when(repository.isDatabaseAvailable()).thenReturn(true);
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("log-analyzer"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void reportsFailedDatabaseProbe() throws Exception {
        when(repository.isDatabaseAvailable()).thenReturn(false);
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void handlesDatabaseFailureWithoutExposingDetails() throws Exception {
        when(repository.isDatabaseAvailable())
                .thenThrow(new DataAccessResourceFailureException("private connection details"));
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.database").value("DOWN"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }
}