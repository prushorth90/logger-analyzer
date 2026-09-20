package dev.loganalyzer.service;

import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.entity.Alert;
import dev.loganalyzer.entity.AlertRule;
import dev.loganalyzer.repository.AlertRepository;
import dev.loganalyzer.repository.AlertRuleRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertServiceTest {
    @Test
    void pausingRuleResolvesItsActiveAlert() {
        AlertRuleRepository rules = mock(AlertRuleRepository.class);
        AlertRepository alerts = mock(AlertRepository.class);
        AlertService service = new AlertService(rules, alerts);
        UUID ruleId = UUID.randomUUID();
        AlertRule rule = mock(AlertRule.class);
        Alert alert = mock(Alert.class);
        when(rules.findById(ruleId)).thenReturn(Optional.of(rule));
        when(alerts.findFirstByRuleIdAndStatusIn(any(), any())).thenReturn(Optional.of(alert));

        service.setRuleActive(ruleId, false);

        verify(rule).setActive(false);
        verify(alert).resolve(any(), org.mockito.ArgumentMatchers.eq("Rule paused"));
    }
}