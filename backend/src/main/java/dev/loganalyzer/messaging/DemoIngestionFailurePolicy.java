package dev.loganalyzer.messaging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoIngestionFailurePolicy {
    public static final String METADATA_KEY = "demoFailure";
    public static final String METADATA_VALUE = "retry-to-dlq";

    private final boolean enabled;

    public DemoIngestionFailurePolicy(
            @Value("${log-analyzer.demo.ingestion-failures-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void failIfRequested(LogRawEventV1 event) {
        if (enabled && event.metadata() != null
                && METADATA_VALUE.equals(event.metadata().get(METADATA_KEY))) {
            throw new DemoIngestionException(
                    "Demo ingestion failure requested; retrying before logs.raw.dlq");
        }
    }

    public LogRawEventV1 prepareManualReplay(LogRawEventV1 event) {
        if (event.metadata() == null || !METADATA_VALUE.equals(event.metadata().get(METADATA_KEY))) {
            return event;
        }
        Map<String, Object> metadata = new LinkedHashMap<>(event.metadata());
        metadata.remove(METADATA_KEY);
        return new LogRawEventV1(event.schemaVersion(), event.eventId(), event.correlationId(), event.timestamp(),
                event.serviceName(), event.environment(), event.severity(), event.message(), event.traceId(),
                event.host(), Collections.unmodifiableMap(metadata));
    }
}