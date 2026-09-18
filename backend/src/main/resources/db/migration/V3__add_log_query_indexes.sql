CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_log_entries_environment ON log_entries (environment);
CREATE INDEX idx_log_entries_message_trgm ON log_entries USING GIN (LOWER(message) gin_trgm_ops);