package dev.loganalyzer.repository;

import java.util.List;
import java.util.UUID;

import dev.loganalyzer.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {
    List<AlertRule> findAllByOrderByNameAsc();
    List<AlertRule> findByActiveTrueOrderByNameAsc();
}