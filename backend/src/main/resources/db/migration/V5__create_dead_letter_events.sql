CREATE TABLE dead_letter_events (
    id UUID PRIMARY KEY,
    ingestion_event_id UUID NOT NULL UNIQUE,
    correlation_id VARCHAR(255) NOT NULL,
    original_event JSONB NOT NULL,
    failure_reason TEXT NOT NULL,
    retry_count INTEGER NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL,
    manual_retry_count INTEGER NOT NULL DEFAULT 0,
    last_retried_at TIMESTAMPTZ
);

CREATE INDEX idx_dead_letter_events_failed_at
    ON dead_letter_events (failed_at DESC);