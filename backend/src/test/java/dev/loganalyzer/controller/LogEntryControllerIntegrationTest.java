package dev.loganalyzer.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.messaging.LogIngestionPublisher;
import dev.loganalyzer.messaging.LogRawEventV1;
import dev.loganalyzer.repository.LogEntryRepository;
import dev.loganalyzer.search.LogSearchResult;
import dev.loganalyzer.search.OpenSearchLogIndex;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:4.1.0");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("log-analyzer.opensearch.indexing-enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogEntryRepository repository;

    @Autowired
    private KafkaTemplate<String, LogRawEventV1> kafkaTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private OpenSearchLogIndex logIndex;

    @BeforeEach
    void clearLogs() {
        repository.deleteAll();
    }

    @Test
    void acceptsAndEventuallyPersistsLogEntry() throws Exception {
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "correlation-123")
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
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", "correlation-123"))
                .andExpect(jsonPath("$.eventId").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").value("correlation-123"))
                .andExpect(jsonPath("$.status").value("accepted"));

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(repository.findAll()).singleElement().satisfies(logEntry -> {
                    assertThat(logEntry.getServiceName()).isEqualTo("billing-api");
                    assertThat(logEntry.getSeverity()).isEqualTo(Severity.ERROR);
                    assertThat(logEntry.getMessage()).isEqualTo("Payment failed");
                }));
    }

    @Test
    void persistsRepeatedKafkaDeliveryOnlyOnceAndCountsDuplicates() {
        UUID eventId = UUID.randomUUID();
        LogRawEventV1 event = new LogRawEventV1(LogRawEventV1.SCHEMA_VERSION, eventId, "duplicate-test",
                Instant.parse("2026-09-17T12:00:00Z"), "billing-api", "test", Severity.ERROR,
                "Repeated delivery", null, "billing-01", Map.of("source", "integration-test"));
        double duplicateCountBefore = meterRegistry.counter("log.ingestion.duplicates").count();

        for (int delivery = 0; delivery < 5; delivery++) {
            kafkaTemplate.send(LogIngestionPublisher.TOPIC, "delivery-" + delivery, event).join();
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(repository.findAll())
                    .filteredOn(logEntry -> eventId.equals(logEntry.getIngestionEventId()))
                    .hasSize(1);
            assertThat(meterRegistry.counter("log.ingestion.duplicates").count())
                    .isEqualTo(duplicateCountBefore + 4);
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

            @Test
            void aggregatesOverviewWithinSelectedTimePeriod() throws Exception {
            repository.saveAll(List.of(
                log("2026-09-17T09:59:59Z", "outside-api", "production", Severity.ERROR, "Before", null),
                log("2026-09-17T10:00:00Z", "billing-api", "production", Severity.ERROR, "Failed", null),
                log("2026-09-17T10:30:00Z", "billing-api", "production", Severity.WARN, "Slow", null),
                log("2026-09-17T11:15:00Z", "orders-api", "production", Severity.ERROR, "Failed", null),
                log("2026-09-17T11:45:00Z", "orders-api", "production", Severity.INFO, "Accepted", null),
                log("2026-09-17T12:00:00Z", "outside-api", "production", Severity.ERROR, "After", null)));

            mockMvc.perform(get("/api/logs/overview")
                    .param("startTimestamp", "2026-09-17T10:00:00Z")
                    .param("endTimestamp", "2026-09-17T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").value(4))
                .andExpect(jsonPath("$.errorCount").value(2))
                .andExpect(jsonPath("$.warningCount").value(1))
                .andExpect(jsonPath("$.activeServices").value(2))
                .andExpect(jsonPath("$.logsByService[0].name").value("billing-api"))
                .andExpect(jsonPath("$.logsByService[0].count").value(2))
                .andExpect(jsonPath("$.logsBySeverity.length()").value(3))
                .andExpect(jsonPath("$.logsOverTime[0].timestamp").value("2026-09-17T10:00:00Z"))
                .andExpect(jsonPath("$.logsOverTime[0].count").value(2))
                .andExpect(jsonPath("$.logsOverTime[1].timestamp").value("2026-09-17T11:00:00Z"))
                .andExpect(jsonPath("$.errorsByService[0].name").value("billing-api"))
                .andExpect(jsonPath("$.errorsByService[0].count").value(1));
            }

    @Test
    void rejectsInvalidOverviewTimePeriod() throws Exception {
        mockMvc.perform(get("/api/logs/overview")
                        .param("startTimestamp", "2026-09-17T12:00:00Z")
                        .param("endTimestamp", "2026-09-17T12:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTimestamp must be before endTimestamp"));
    }

            @Test
            void filtersLogsByCombinedCriteria() throws Exception {
            List<LogEntry> savedLogs = repository.saveAll(List.of(
                log("2026-09-17T12:00:00Z", "billing-api", "production", Severity.ERROR,
                    "Payment FAILED for order 42", "trace-123"),
                log("2026-09-17T12:05:00Z", "billing-api", "production", Severity.INFO,
                    "Payment accepted", "trace-123"),
                log("2026-09-17T12:10:00Z", "orders-api", "staging", Severity.ERROR,
                    "Payment failed", "trace-999")));
            when(logIndex.search(any(), any())).thenReturn(new LogSearchResult(
                    List.of(savedLogs.getFirst().getIngestionEventId()), 1));

            mockMvc.perform(get("/api/logs")
                    .param("serviceName", "billing-api")
                    .param("environment", "production")
                    .param("severity", "ERROR")
                    .param("traceId", "trace-123")
                    .param("startTimestamp", "2026-09-17T11:59:00Z")
                    .param("endTimestamp", "2026-09-17T12:01:00Z")
                    .param("search", "failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Payment FAILED for order 42"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalRecords").value(1));
            }

            @Test
            void paginatesWithNewestLogsFirstByDefault() throws Exception {
            repository.saveAll(List.of(
                log("2026-09-17T12:00:00Z", "billing-api", "production", Severity.INFO, "Oldest", null),
                log("2026-09-17T12:05:00Z", "orders-api", "production", Severity.WARN, "Middle", null),
                log("2026-09-17T12:10:00Z", "auth-api", "production", Severity.ERROR, "Newest", null)));

            mockMvc.perform(get("/api/logs").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].message").value("Newest"))
                .andExpect(jsonPath("$.content[1].message").value("Middle"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalRecords").value(3));
            }

            @Test
            void sortsByRequestedFieldAndDirection() throws Exception {
            repository.saveAll(List.of(
                log("2026-09-17T12:00:00Z", "orders-api", "production", Severity.INFO, "Orders", null),
                log("2026-09-17T12:05:00Z", "auth-api", "production", Severity.INFO, "Auth", null)));

            mockMvc.perform(get("/api/logs").param("sort", "serviceName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serviceName").value("auth-api"))
                .andExpect(jsonPath("$.content[1].serviceName").value("orders-api"));
            }

            private LogEntry log(String timestamp, String serviceName, String environment, Severity severity,
                String message, String traceId) {
            return new LogEntry(UUID.randomUUID(), Instant.parse(timestamp), serviceName, environment, severity,
                message, traceId, serviceName + "-01", Map.of());
            }
}