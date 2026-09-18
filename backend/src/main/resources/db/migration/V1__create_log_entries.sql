CREATE TABLE log_entries (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    level VARCHAR(16) NOT NULL,
    message TEXT NOT NULL
);