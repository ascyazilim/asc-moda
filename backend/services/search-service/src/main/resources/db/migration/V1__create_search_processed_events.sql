CREATE TABLE search_processed_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(160) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(1000),
    processed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_search_processed_events_event_id UNIQUE (event_id),
    CONSTRAINT ck_search_processed_events_status CHECK (status IN ('PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_search_processed_events_status ON search_processed_events (status);
CREATE INDEX idx_search_processed_events_event_type ON search_processed_events (event_type);
CREATE INDEX idx_search_processed_events_processed_at ON search_processed_events (processed_at);
CREATE INDEX idx_search_processed_events_created_at ON search_processed_events (created_at);
CREATE INDEX idx_search_processed_events_reference ON search_processed_events (reference_type, reference_id);
