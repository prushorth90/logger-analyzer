ALTER TABLE log_entries
    ADD COLUMN ingestion_event_id UUID;

CREATE UNIQUE INDEX uq_log_entries_ingestion_event_id
    ON log_entries (ingestion_event_id)
    WHERE ingestion_event_id IS NOT NULL;