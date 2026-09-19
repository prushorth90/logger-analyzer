package dev.loganalyzer.search;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import dev.loganalyzer.messaging.LogPersistedEventV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenSearchLogIndex {
    private final RestClient restClient;
    private final String indexName;
    private volatile boolean initialized;

    public OpenSearchLogIndex(
            RestClient.Builder restClientBuilder,
            @Value("${log-analyzer.opensearch.url}") String url,
            @Value("${log-analyzer.opensearch.index-name}") String indexName) {
        this.restClient = restClientBuilder.baseUrl(url).build();
        this.indexName = indexName;
    }

    public void index(LogPersistedEventV1 event) {
        ensureIndex();
        restClient.put()
                .uri("/{index}/_doc/{eventId}", indexName, event.eventId())
                .body(Map.of(
                        "eventId", event.eventId(),
                        "timestamp", event.timestamp(),
                        "message", event.message(),
                        "serviceName", event.serviceName(),
                        "severity", event.severity().name(),
                        "traceId", event.traceId() == null ? "" : event.traceId(),
                        "environment", event.environment()))
                .retrieve()
                .toBodilessEntity();
    }

    public LogSearchResult search(LogSearchCriteria criteria, Pageable pageable) {
        ensureIndex();
        JsonNode response = restClient.post()
                .uri("/{index}/_search", indexName)
                .body(searchRequest(criteria, pageable))
                .retrieve()
                .body(JsonNode.class);

        List<UUID> eventIds = new ArrayList<>();
        if (response != null) {
            response.path("hits").path("hits").forEach(hit -> eventIds.add(UUID.fromString(hit.path("_id").asText())));
        }
        long totalHits = response == null ? 0 : response.path("hits").path("total").path("value").asLong();
        return new LogSearchResult(eventIds, totalHits);
    }

    private Map<String, Object> searchRequest(LogSearchCriteria criteria, Pageable pageable) {
        List<Map<String, Object>> filters = new ArrayList<>();
        addTermFilter(filters, "serviceName", criteria.serviceName());
        addTermFilter(filters, "environment", criteria.environment());
        addTermFilter(filters, "severity", criteria.severity() == null ? null : criteria.severity().name());
        addTermFilter(filters, "traceId", criteria.traceId());

        Map<String, Object> range = new LinkedHashMap<>();
        if (criteria.startTimestamp() != null) {
            range.put("gte", DateTimeFormatter.ISO_INSTANT.format(criteria.startTimestamp()));
        }
        if (criteria.endTimestamp() != null) {
            range.put("lte", DateTimeFormatter.ISO_INSTANT.format(criteria.endTimestamp()));
        }
        if (!range.isEmpty()) {
            filters.add(Map.of("range", Map.of("timestamp", range)));
        }

        Map<String, Object> boolQuery = new LinkedHashMap<>();
        boolQuery.put("must", List.of(Map.of("multi_match", Map.of(
                "query", criteria.text().trim(),
                "fields", List.of("message", "serviceName.search", "severity.search", "traceId.search",
                        "environment.search"),
                "type", "best_fields"))));
        boolQuery.put("filter", filters);

        return Map.of(
                "from", pageable.getOffset(),
                "size", pageable.getPageSize(),
                "track_total_hits", true,
                "_source", false,
                "query", Map.of("bool", boolQuery),
                "sort", List.of(Map.of("timestamp", Map.of("order", "desc")), Map.of("eventId", "asc")));
    }

    private void addTermFilter(List<Map<String, Object>> filters, String field, String value) {
        if (StringUtils.hasText(value)) {
            filters.add(Map.of("term", Map.of(field, value)));
        }
    }

    private void ensureIndex() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                restClient.head().uri("/{index}", indexName).retrieve().toBodilessEntity();
            } catch (RestClientResponseException exception) {
                if (!HttpStatusCode.valueOf(404).equals(exception.getStatusCode())) {
                    throw exception;
                }
                createIndex();
            }
            initialized = true;
        }
    }

    private void createIndex() {
        Map<String, Object> searchableKeyword = Map.of(
                "type", "keyword",
                "fields", Map.of("search", Map.of("type", "text")));
        restClient.put()
                .uri("/{index}", indexName)
                .body(Map.of("mappings", Map.of(
                        "dynamic", "strict",
                        "properties", Map.of(
                                "eventId", Map.of("type", "keyword"),
                                "timestamp", Map.of("type", "date"),
                                "message", Map.of("type", "text"),
                                "serviceName", searchableKeyword,
                                "severity", searchableKeyword,
                                "traceId", searchableKeyword,
                                "environment", searchableKeyword))))
                .retrieve()
                .toBodilessEntity();
    }
}