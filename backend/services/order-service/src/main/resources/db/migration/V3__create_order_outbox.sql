CREATE TABLE order_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    correlation_id VARCHAR(160) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_order_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_order_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_order_outbox_status_occurred_at ON order_outbox_events (status, occurred_at);
CREATE INDEX idx_order_outbox_published_at ON order_outbox_events (published_at);
CREATE INDEX idx_order_outbox_aggregate ON order_outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_order_outbox_event_type ON order_outbox_events (event_type);
CREATE INDEX idx_order_outbox_correlation_id ON order_outbox_events (correlation_id);
