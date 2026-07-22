-- Phase 3 technical foundations migration (immutable after acceptance)
-- Ownership: securepay-core platform persistence foundations

CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS events;
CREATE SCHEMA IF NOT EXISTS idempotency;

COMMENT ON SCHEMA platform IS 'Platform-owned technical metadata and test scaffolding';
COMMENT ON SCHEMA audit IS 'Append-only immutable audit events';
COMMENT ON SCHEMA events IS 'Transactional outbox for domain events';
COMMENT ON SCHEMA idempotency IS 'Persistent idempotency records';

-- Relocate Phase 2 platform metadata into the platform schema when present in public.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'platform_metadata'
    ) THEN
        ALTER TABLE public.platform_metadata SET SCHEMA platform;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS platform.platform_metadata (
    metadata_key   VARCHAR(128) PRIMARY KEY,
    metadata_value TEXT        NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

INSERT INTO platform.platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'phase-03-database-audit-idempotency-foundation')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = (NOW() AT TIME ZONE 'UTC');

CREATE TABLE IF NOT EXISTS platform.technical_test_records (
    id             UUID PRIMARY KEY,
    record_key     VARCHAR(128) NOT NULL UNIQUE,
    payload        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE IF NOT EXISTS audit.audit_events (
    id                 UUID PRIMARY KEY,
    event_id           VARCHAR(128) NOT NULL UNIQUE,
    category           VARCHAR(64)  NOT NULL,
    event_type         VARCHAR(128) NOT NULL,
    actor_type         VARCHAR(64)  NOT NULL,
    actor_id           VARCHAR(128),
    actor_ks_number    VARCHAR(32),
    application_id     VARCHAR(128),
    resource_type      VARCHAR(128) NOT NULL,
    resource_id        VARCHAR(128) NOT NULL,
    action             VARCHAR(128) NOT NULL,
    previous_state     JSONB,
    new_state          JSONB,
    reason             TEXT,
    request_id         VARCHAR(128) NOT NULL,
    correlation_id     VARCHAR(128) NOT NULL,
    source_service     VARCHAR(128) NOT NULL,
    occurred_at        TIMESTAMPTZ  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    metadata           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    integrity_version  INTEGER      NOT NULL DEFAULT 1,
    integrity_hash     VARCHAR(128),
    CONSTRAINT audit_events_occurred_before_created CHECK (occurred_at <= created_at),
    CONSTRAINT audit_events_integrity_version_non_negative CHECK (integrity_version >= 0)
);

CREATE INDEX IF NOT EXISTS idx_audit_events_event_id ON audit.audit_events (event_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource ON audit.audit_events (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit.audit_events (actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_correlation_id ON audit.audit_events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_at ON audit.audit_events (occurred_at);

CREATE OR REPLACE FUNCTION audit.prevent_audit_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit.audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_events_no_update ON audit.audit_events;
CREATE TRIGGER trg_audit_events_no_update
    BEFORE UPDATE ON audit.audit_events
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_audit_mutation();

DROP TRIGGER IF EXISTS trg_audit_events_no_delete ON audit.audit_events;
CREATE TRIGGER trg_audit_events_no_delete
    BEFORE DELETE ON audit.audit_events
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_audit_mutation();

CREATE TABLE IF NOT EXISTS idempotency.idempotency_records (
    id                    UUID PRIMARY KEY,
    application_id        VARCHAR(128),
    actor_id              VARCHAR(128),
    idempotency_key       VARCHAR(128) NOT NULL,
    operation_code        VARCHAR(128) NOT NULL,
    request_hash          VARCHAR(128) NOT NULL,
    request_content_type  VARCHAR(128) NOT NULL,
    resource_type         VARCHAR(128),
    resource_id             VARCHAR(128),
    processing_status     VARCHAR(32)  NOT NULL,
    response_status       INTEGER,
    response_content_type VARCHAR(128),
    response_body         JSONB,
    failure_code          VARCHAR(128),
    locked_until          TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    completed_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT idempotency_records_status_check CHECK (
        processing_status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED_RETRYABLE', 'FAILED_FINAL', 'EXPIRED')
    ),
    CONSTRAINT idempotency_records_version_non_negative CHECK (version >= 0),
    CONSTRAINT idempotency_records_expires_after_created CHECK (expires_at >= created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_idempotency_scope
    ON idempotency.idempotency_records (
        COALESCE(application_id, ''),
        COALESCE(actor_id, ''),
        operation_code,
        idempotency_key
    );

CREATE INDEX IF NOT EXISTS idx_idempotency_status ON idempotency.idempotency_records (processing_status);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency.idempotency_records (expires_at);
CREATE INDEX IF NOT EXISTS idx_idempotency_created_at ON idempotency.idempotency_records (created_at);

CREATE TABLE IF NOT EXISTS events.outbox_events (
    id              UUID PRIMARY KEY,
    event_id        VARCHAR(128) NOT NULL UNIQUE,
    aggregate_type  VARCHAR(128) NOT NULL,
    aggregate_id    VARCHAR(128) NOT NULL,
    event_type      VARCHAR(256) NOT NULL,
    event_version   VARCHAR(32)  NOT NULL,
    payload         JSONB        NOT NULL,
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    correlation_id  VARCHAR(128) NOT NULL,
    causation_id    VARCHAR(128),
    actor_type      VARCHAR(64)  NOT NULL,
    actor_id        VARCHAR(128),
    source_service  VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    available_at    TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT outbox_events_status_check CHECK (
        status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'DEAD_LETTER')
    ),
    CONSTRAINT outbox_events_attempt_count_non_negative CHECK (attempt_count >= 0),
    CONSTRAINT outbox_events_version_non_negative CHECK (version >= 0),
    CONSTRAINT outbox_events_published_after_created CHECK (
        published_at IS NULL OR published_at >= created_at
    )
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_available_at ON events.outbox_events (status, available_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON events.outbox_events (aggregate_type, aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON events.outbox_events (created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_correlation_id ON events.outbox_events (correlation_id);
