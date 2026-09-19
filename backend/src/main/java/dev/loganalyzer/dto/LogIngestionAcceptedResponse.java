package dev.loganalyzer.dto;

import java.util.UUID;

public record LogIngestionAcceptedResponse(UUID eventId, String correlationId, String status) {
}