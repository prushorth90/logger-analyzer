package dev.loganalyzer.search;

import java.util.List;
import java.util.UUID;

public record LogSearchResult(List<UUID> eventIds, long totalHits) {
}