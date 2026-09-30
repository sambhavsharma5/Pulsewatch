CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE monitors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    url TEXT NOT NULL,
    interval_seconds INT NOT NULL DEFAULT 60 CHECK (interval_seconds >= 5),
    timeout_seconds INT NOT NULL DEFAULT 5 CHECK (timeout_seconds BETWEEN 1 AND 30),
    expected_status_code INT NOT NULL DEFAULT 200,
    webhook_url TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UP', 'DOWN')),
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_checked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_monitors_scheduler ON monitors(last_checked_at NULLS FIRST, interval_seconds);

CREATE TABLE heartbeats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status_code INT,
    latency_ms INT NOT NULL,
    is_successful BOOLEAN NOT NULL,
    ssl_days_remaining INT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_heartbeats_monitor_created ON heartbeats(monitor_id, created_at DESC);

CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    cause TEXT NOT NULL
);

CREATE INDEX idx_incidents_active ON incidents(monitor_id) WHERE resolved_at IS NULL;