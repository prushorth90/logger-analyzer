package dev.loganalyzer.repository;

import java.util.Optional;
import java.util.UUID;

import dev.loganalyzer.entity.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {
    Optional<DeadLetterEvent> findByIngestionEventId(UUID ingestionEventId);
}