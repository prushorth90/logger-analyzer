package dev.loganalyzer.repository;

import java.util.UUID;

import dev.loganalyzer.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {
}