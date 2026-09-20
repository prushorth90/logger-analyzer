package dev.loganalyzer.repository;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.UUID;

import dev.loganalyzer.entity.LogEntry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogEntryRepository extends JpaRepository<LogEntry, UUID>, JpaSpecificationExecutor<LogEntry> {
	List<LogEntry> findByIngestionEventIdIn(Collection<UUID> ingestionEventIds);

	List<LogEntry> findByTraceIdOrderByTimestampAscIdAsc(String traceId);

	long countByServiceNameAndSeverityAndTimestampGreaterThanEqualAndTimestampLessThan(
			String serviceName, dev.loganalyzer.entity.Severity severity, Instant startTimestamp, Instant endTimestamp);

	@Modifying
	@Query(value = """
		INSERT INTO log_entries (
			id, ingestion_event_id, timestamp, service_name, environment, severity, message, trace_id, host, metadata
		) VALUES (
			:id, :ingestionEventId, :timestamp, :serviceName, :environment, :severity,
			:message, :traceId, :host, CAST(:metadata AS jsonb)
		)
		ON CONFLICT (ingestion_event_id) WHERE ingestion_event_id IS NOT NULL DO NOTHING
		""", nativeQuery = true)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("ingestionEventId") UUID ingestionEventId,
			@Param("timestamp") Instant timestamp,
			@Param("serviceName") String serviceName,
			@Param("environment") String environment,
			@Param("severity") String severity,
			@Param("message") String message,
			@Param("traceId") String traceId,
			@Param("host") String host,
			@Param("metadata") String metadata);

    @Query(value = """
	    SELECT COUNT(*) AS totalLogs,
		   COUNT(*) FILTER (WHERE severity = 'ERROR') AS errorCount,
		   COUNT(*) FILTER (WHERE severity = 'WARN') AS warningCount,
		   COUNT(DISTINCT service_name) AS activeServices
	    FROM log_entries
	    WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp
	    """, nativeQuery = true)
    LogOverviewSummary summarize(
	    @Param("startTimestamp") Instant startTimestamp,
	    @Param("endTimestamp") Instant endTimestamp);

    @Query(value = """
	    SELECT service_name AS name, COUNT(*) AS count
	    FROM log_entries
	    WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp
	    GROUP BY service_name
	    ORDER BY count DESC, service_name ASC
	    """, nativeQuery = true)
    List<NamedCountProjection> countByService(
	    @Param("startTimestamp") Instant startTimestamp,
	    @Param("endTimestamp") Instant endTimestamp);

    @Query(value = """
	    SELECT severity AS name, COUNT(*) AS count
	    FROM log_entries
	    WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp
	    GROUP BY severity
	    ORDER BY count DESC, severity ASC
	    """, nativeQuery = true)
    List<NamedCountProjection> countBySeverity(
	    @Param("startTimestamp") Instant startTimestamp,
	    @Param("endTimestamp") Instant endTimestamp);

    @Query(value = """
	    SELECT date_trunc('hour', timestamp) AS timestamp, COUNT(*) AS count
	    FROM log_entries
	    WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp
	    GROUP BY date_trunc('hour', timestamp)
	    ORDER BY timestamp ASC
	    """, nativeQuery = true)
    List<TimeCountProjection> countByHour(
	    @Param("startTimestamp") Instant startTimestamp,
	    @Param("endTimestamp") Instant endTimestamp);

    @Query(value = """
	    SELECT service_name AS name, COUNT(*) AS count
	    FROM log_entries
	    WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp AND severity = 'ERROR'
	    GROUP BY service_name
	    ORDER BY count DESC, service_name ASC
	    """, nativeQuery = true)
    List<NamedCountProjection> countErrorsByService(
	    @Param("startTimestamp") Instant startTimestamp,
	    @Param("endTimestamp") Instant endTimestamp);
}