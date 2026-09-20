package dev.loganalyzer.controller;

import java.util.List;
import java.util.UUID;

import dev.loganalyzer.dto.AlertResponse;
import dev.loganalyzer.dto.AlertRuleResponse;
import dev.loganalyzer.dto.CreateAlertRuleRequest;
import dev.loganalyzer.entity.AlertStatus;
import dev.loganalyzer.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping("/rules")
    public List<AlertRuleResponse> findRules() { return service.findRules(); }

    @PostMapping("/rules")
    public ResponseEntity<AlertRuleResponse> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRule(request));
    }

    @PatchMapping("/rules/{id}/active")
    public AlertRuleResponse setRuleActive(@PathVariable UUID id, @RequestParam boolean active) {
        return service.setRuleActive(id, active);
    }

    @GetMapping
    public Page<AlertResponse> findAlerts(
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAlerts(status, pageable);
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable UUID id) { return service.acknowledge(id); }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable UUID id) { return service.resolve(id); }
}