CREATE TABLE alert_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    service_name VARCHAR(255) NOT NULL,
    threshold_count BIGINT NOT NULL,
    window_minutes INTEGER NOT NULL,
    cooldown_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_alert_rules_threshold CHECK (threshold_count >= 1),
    CONSTRAINT chk_alert_rules_window CHECK (window_minutes BETWEEN 1 AND 1440),
    CONSTRAINT chk_alert_rules_cooldown CHECK (cooldown_minutes BETWEEN 1 AND 10080)
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL,
    observed_count BIGINT NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    window_ended_at TIMESTAMPTZ NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    resolution_reason VARCHAR(255),
    CONSTRAINT chk_alerts_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
);

CREATE UNIQUE INDEX uq_alerts_active_rule
    ON alerts (alert_rule_id)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE INDEX idx_alert_rules_active ON alert_rules (active) WHERE active = TRUE;
CREATE INDEX idx_alerts_status_opened_at ON alerts (status, opened_at DESC);
CREATE INDEX idx_alerts_rule_opened_at ON alerts (alert_rule_id, opened_at DESC);