package dev.loganalyzer.dto;

import java.util.List;

public record PagedDeadLetterEventResponse(
        List<DeadLetterEventResponse> content,
        int pageNumber,
        int pageSize,
        int totalPages,
        long totalRecords) {
}