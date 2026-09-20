package dev.loganalyzer.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.loganalyzer.dto.AlertResponse;
import dev.loganalyzer.dto.AlertRuleResponse;
import dev.loganalyzer.dto.CreateAlertRuleRequest;
import dev.loganalyzer.entity.Alert;
import dev.loganalyzer.entity.AlertRule;
import dev.loganalyzer.entity.AlertStatus;
import dev.loganalyzer.repository.AlertRepository;
import dev.loganalyzer.repository.AlertRuleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertService {
    private static final List<AlertStatus> ACTIVE_STATUSES =
            List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);
    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;

    public AlertService(AlertRuleRepository ruleRepository, AlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> findRules() {
        return ruleRepository.findAllByOrderByNameAsc().stream().map(this::toRuleResponse).toList();
    }

    @Transactional
    public AlertRuleResponse createRule(CreateAlertRuleRequest request) {
        try {
            return toRuleResponse(ruleRepository.saveAndFlush(new AlertRule(request, Instant.now())));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An alert rule with this name already exists");
        }
    }

    @Transactional
    public AlertRuleResponse setRuleActive(UUID id, boolean active) {
        AlertRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert rule not found"));
        rule.setActive(active);
        if (!active) {
            alertRepository.findFirstByRuleIdAndStatusIn(id, ACTIVE_STATUSES)
                .ifPresent(alert -> alert.resolve(Instant.now(), "Rule paused"));
        }
        return toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> findAlerts(AlertStatus status, Pageable pageable) {
        Page<Alert> alerts = status == null
                ? alertRepository.findAllByOrderByOpenedAtDesc(pageable)
                : alertRepository.findByStatusOrderByOpenedAtDesc(status, pageable);
        return alerts.map(this::toAlertResponse);
    }

    @Transactional
    public AlertResponse acknowledge(UUID id) {
        Alert alert = findAlert(id);
        alert.acknowledge(Instant.now());
        return toAlertResponse(alert);
    }

    @Transactional
    public AlertResponse resolve(UUID id) {
        Alert alert = findAlert(id);
        alert.resolve(Instant.now(), "Resolved by operator");
        return toAlertResponse(alert);
    }

    private Alert findAlert(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    AlertRuleResponse toRuleResponse(AlertRule rule) {
        return new AlertRuleResponse(rule.getId(), rule.getName(), rule.getServiceName(), rule.getThresholdCount(),
                rule.getWindowMinutes(), rule.getCooldownMinutes(), rule.isActive(), rule.getCreatedAt());
    }

    AlertResponse toAlertResponse(Alert alert) {
        AlertRule rule = alert.getRule();
        return new AlertResponse(alert.getId(), rule.getId(), rule.getName(), rule.getServiceName(), alert.getStatus(),
                rule.getThresholdCount(), alert.getObservedCount(), rule.getWindowMinutes(), alert.getWindowStartedAt(),
                alert.getWindowEndedAt(), alert.getOpenedAt(), alert.getAcknowledgedAt(), alert.getResolvedAt(),
                alert.getResolutionReason());
    }
}