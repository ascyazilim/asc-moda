CREATE TABLE notification_messages (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    correlation_id VARCHAR(160) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(240) NOT NULL,
    subject VARCHAR(240) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    source_service VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    failure_reason VARCHAR(1000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_notification_messages_event_id UNIQUE (event_id),
    CONSTRAINT ck_notification_messages_status CHECK (status IN ('RECEIVED', 'PENDING', 'SENT', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_notification_messages_channel CHECK (channel IN ('EMAIL', 'SMS')),
    CONSTRAINT ck_notification_messages_type CHECK (notification_type IN ('ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_CANCELLED')),
    CONSTRAINT ck_notification_messages_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_notification_messages_status ON notification_messages (status);
CREATE INDEX idx_notification_messages_type ON notification_messages (notification_type);
CREATE INDEX idx_notification_messages_channel ON notification_messages (channel);
CREATE INDEX idx_notification_messages_reference ON notification_messages (reference_type, reference_id);
CREATE INDEX idx_notification_messages_created_at ON notification_messages (created_at);
CREATE INDEX idx_notification_messages_correlation_id ON notification_messages (correlation_id);
