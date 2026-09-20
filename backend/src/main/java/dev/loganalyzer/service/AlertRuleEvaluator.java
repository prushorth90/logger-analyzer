package dev.loganalyzer.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.loganalyzer.entity.Alert;
import dev.loganalyzer.entity.AlertRule;
import dev.loganalyzer.entity.AlertStatus;
import dev.loganalyzer.entity.Severity;
import dev.loganalyzer.repository.AlertRepository;
import dev.loganalyzer.repository.AlertRuleRepository;
import dev.loganalyzer.repository.LogEntryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertRuleEvaluator {
    private static final List<AlertStatus> ACTIVE_STATUSES =
            List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final LogEntryRepository logEntryRepository;

    public AlertRuleEvaluator(AlertRuleRepository ruleRepository, AlertRepository alertRepository,
            LogEntryRepository logEntryRepository) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
        this.logEntryRepository = logEntryRepository;
    }

    @Scheduled(fixedDelayString = "${log-analyzer.alerts.evaluation-interval:30s}")
    @Transactional
    public void evaluate() {
        evaluateAt(Instant.now());
    }

    void evaluateAt(Instant now) {
        ruleRepository.findByActiveTrueOrderByNameAsc().forEach(rule -> evaluateRule(rule, now));
    }

    private void evaluateRule(AlertRule rule, Instant now) {
        Instant windowStart = now.minus(Duration.ofMinutes(rule.getWindowMinutes()));
        long count = logEntryRepository.countByServiceNameAndSeverityAndTimestampGreaterThanEqualAndTimestampLessThan(
                rule.getServiceName(), Severity.ERROR, windowStart, now);
        Alert activeAlert = alertRepository.findFirstByRuleIdAndStatusIn(rule.getId(), ACTIVE_STATUSES).orElse(null);

        if (count > rule.getThresholdCount()) {
            if (activeAlert == null && cooldownExpired(rule, now)) {
                alertRepository.insertOpenIfAbsent(UUID.randomUUID(), rule.getId(), count, windowStart, now);
            }
        } else if (activeAlert != null) {
            activeAlert.resolve(now, "Condition cleared");
        }
    }

    private boolean cooldownExpired(AlertRule rule, Instant now) {
        return alertRepository.findFirstByRuleIdAndStatusOrderByResolvedAtDesc(rule.getId(), AlertStatus.RESOLVED)
                .map(alert -> !now.isBefore(alert.getResolvedAt().plus(Duration.ofMinutes(rule.getCooldownMinutes()))))
                .orElse(true);
    }
}