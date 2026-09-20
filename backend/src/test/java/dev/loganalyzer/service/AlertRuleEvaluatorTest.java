package dev.loganalyzer.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.entity.Alert;
import dev.loganalyzer.entity.AlertRule;
import dev.loganalyzer.entity.AlertStatus;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.repository.AlertRepository;
import dev.loganalyzer.repository.AlertRuleRepository;
import dev.loganalyzer.repository.LogEntryRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertRuleEvaluatorTest {
    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Test
    void opensOneAlertWhenThresholdIsExceeded() {
        Fixture fixture = fixture(25, Optional.empty(), Optional.empty());

        fixture.evaluator.evaluateAt(NOW);

        verify(fixture.alerts).insertOpenIfAbsent(any(), any(), eq(25L), eq(NOW.minusSeconds(300)), eq(NOW));
    }

    @Test
    void doesNotDuplicateAnActiveAlert() {
        Alert active = mock(Alert.class);
        Fixture fixture = fixture(25, Optional.of(active), Optional.empty());

        fixture.evaluator.evaluateAt(NOW);

        verify(fixture.alerts, never()).insertOpenIfAbsent(any(), any(), anyLong(), any(), any());
    }

    @Test
    void resolvesActiveAlertWhenConditionClears() {
        Alert active = mock(Alert.class);
        Fixture fixture = fixture(20, Optional.of(active), Optional.empty());

        fixture.evaluator.evaluateAt(NOW);

        verify(active).resolve(NOW, "Condition cleared");
    }

    @Test
    void suppressesNewAlertDuringResolvedCooldown() {
        Alert resolved = mock(Alert.class);
        when(resolved.getResolvedAt()).thenReturn(NOW.minusSeconds(60));
        Fixture fixture = fixture(25, Optional.empty(), Optional.of(resolved));

        fixture.evaluator.evaluateAt(NOW);

        verify(fixture.alerts, never()).insertOpenIfAbsent(any(), any(), anyLong(), any(), any());
    }

    private Fixture fixture(long observedCount, Optional<Alert> active, Optional<Alert> resolved) {
        AlertRule rule = mock(AlertRule.class);
        UUID ruleId = UUID.randomUUID();
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getServiceName()).thenReturn("payment-service");
        when(rule.getThresholdCount()).thenReturn(20L);
        when(rule.getWindowMinutes()).thenReturn(5);
        when(rule.getCooldownMinutes()).thenReturn(30);
        AlertRuleRepository rules = mock(AlertRuleRepository.class);
        AlertRepository alerts = mock(AlertRepository.class);
        LogEntryRepository logs = mock(LogEntryRepository.class);
        when(rules.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(rule));
        when(alerts.findFirstByRuleIdAndStatusIn(eq(ruleId), any())).thenReturn(active);
        when(alerts.findFirstByRuleIdAndStatusOrderByResolvedAtDesc(ruleId, AlertStatus.RESOLVED))
                .thenReturn(resolved);
        when(logs.countByServiceNameAndSeverityAndTimestampGreaterThanEqualAndTimestampLessThan(
                eq("payment-service"), eq(Severity.ERROR), any(), eq(NOW))).thenReturn(observedCount);
        return new Fixture(alerts, new AlertRuleEvaluator(rules, alerts, logs));
    }

    private record Fixture(AlertRepository alerts, AlertRuleEvaluator evaluator) {
    }
}