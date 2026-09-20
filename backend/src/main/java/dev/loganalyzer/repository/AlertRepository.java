package dev.loganalyzer.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.entity.Alert;
import dev.loganalyzer.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    @Modifying
    @Query(value = """
        INSERT INTO alerts (
            id, alert_rule_id, status, observed_count, window_started_at, window_ended_at, opened_at
        ) VALUES (
            :id, :ruleId, 'OPEN', :observedCount, :windowStartedAt, :windowEndedAt, :windowEndedAt
        )
        ON CONFLICT (alert_rule_id) WHERE status IN ('OPEN', 'ACKNOWLEDGED') DO NOTHING
        """, nativeQuery = true)
    int insertOpenIfAbsent(
            @Param("id") UUID id,
            @Param("ruleId") UUID ruleId,
            @Param("observedCount") long observedCount,
            @Param("windowStartedAt") Instant windowStartedAt,
            @Param("windowEndedAt") Instant windowEndedAt);

    @EntityGraph(attributePaths = "rule")
    Page<Alert> findAllByOrderByOpenedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "rule")
    Page<Alert> findByStatusOrderByOpenedAtDesc(AlertStatus status, Pageable pageable);

    Optional<Alert> findFirstByRuleIdAndStatusIn(UUID ruleId, Collection<AlertStatus> statuses);

    Optional<Alert> findFirstByRuleIdAndStatusOrderByResolvedAtDesc(UUID ruleId, AlertStatus status);

    @Override
    @EntityGraph(attributePaths = "rule")
    Optional<Alert> findById(UUID id);
}