package dev.loganalyzer.service;

import java.time.Instant;
import java.util.Objects;

import dev.loganalyzer.repository.LogEntryRepository;
import dev.loganalyzer.repository.LogOverviewSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(LogOverviewCacheTest.CacheTestConfiguration.class)
class LogOverviewCacheTest {
    private final LogEntryService service;
    private final LogEntryRepository repository;
    private final CacheManager cacheManager;

    @Autowired
    LogOverviewCacheTest(LogEntryService service, LogEntryRepository repository, CacheManager cacheManager) {
        this.service = service;
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void setUp() {
        reset(repository);
        Objects.requireNonNull(cacheManager.getCache("log-overview")).clear();
        when(repository.summarize(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(mock(LogOverviewSummary.class));
    }

    @Test
    void cachesMatchingRangesAndSeparatesDifferentRanges() {
        Instant start = Instant.parse("2026-09-17T10:00:00Z");
        Instant firstEnd = Instant.parse("2026-09-17T11:00:00Z");
        Instant secondEnd = Instant.parse("2026-09-17T12:00:00Z");

        service.getOverview(start, firstEnd);
        service.getOverview(start, firstEnd);
        service.getOverview(start, secondEnd);

        verify(repository, times(1)).summarize(start, firstEnd);
        verify(repository, times(1)).summarize(start, secondEnd);
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean
        LogEntryRepository logEntryRepository() {
            return mock(LogEntryRepository.class);
        }

        @Bean
        LogEntryService logEntryService(LogEntryRepository repository) {
            return new LogEntryService(repository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("log-overview");
        }
    }
}