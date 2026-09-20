CREATE TABLE saved_searches (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    query VARCHAR(500),
    severity VARCHAR(16),
    service_name VARCHAR(255),
    environment VARCHAR(64),
    trace_id VARCHAR(255),
    start_timestamp TIMESTAMPTZ,
    end_timestamp TIMESTAMPTZ,
    sort_direction VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_saved_searches_name UNIQUE (name),
    CONSTRAINT chk_saved_searches_severity
        CHECK (severity IS NULL OR severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
    CONSTRAINT chk_saved_searches_sort_direction
        CHECK (sort_direction IN ('NEWEST', 'OLDEST'))
);

CREATE INDEX idx_saved_searches_created_at ON saved_searches (created_at DESC);