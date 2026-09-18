ALTER TABLE log_entries RENAME COLUMN level TO severity;

ALTER TABLE log_entries
    ADD COLUMN service_name VARCHAR(255) NOT NULL DEFAULT 'unknown',
    ADD COLUMN environment VARCHAR(64) NOT NULL DEFAULT 'unknown',
    ADD COLUMN trace_id VARCHAR(255),
    ADD COLUMN host VARCHAR(255) NOT NULL DEFAULT 'unknown',
    ADD COLUMN metadata JSONB,
    ADD CONSTRAINT chk_log_entries_severity
        CHECK (severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR'));

ALTER TABLE log_entries
    ALTER COLUMN service_name DROP DEFAULT,
    ALTER COLUMN environment DROP DEFAULT,
    ALTER COLUMN host DROP DEFAULT;

CREATE INDEX idx_log_entries_timestamp ON log_entries (timestamp);
CREATE INDEX idx_log_entries_service_name ON log_entries (service_name);
CREATE INDEX idx_log_entries_severity ON log_entries (severity);
CREATE INDEX idx_log_entries_trace_id ON log_entries (trace_id);