package dev.loganalyzer.repository;

import java.time.Instant;
import java.util.Locale;

import dev.loganalyzer.entity.LogEntry;
import dev.loganalyzer.entity.Severity;
import org.springframework.data.jpa.domain.Specification;

public final class LogEntrySpecifications {
    private LogEntrySpecifications() {
    }

    public static Specification<LogEntry> withFilters(
            String serviceName,
            String environment,
            Severity severity,
            String traceId,
            Instant startTimestamp,
            Instant endTimestamp,
            String search) {
        Specification<LogEntry> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (hasText(serviceName)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("serviceName"), serviceName));
        }
        if (hasText(environment)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("environment"), environment));
        }
        if (severity != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("severity"), severity));
        }
        if (hasText(traceId)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("traceId"), traceId));
        }
        if (startTimestamp != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), startTimestamp));
        }
        if (endTimestamp != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), endTimestamp));
        }
        if (hasText(search)) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("message")), pattern, '\\'));
        }

        return specification;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}