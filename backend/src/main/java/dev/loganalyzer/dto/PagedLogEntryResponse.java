package dev.loganalyzer.dto;

import java.util.List;

public record PagedLogEntryResponse(
        List<LogEntryResponse> content,
        int pageNumber,
        int pageSize,
        int totalPages,
        long totalRecords) {
}