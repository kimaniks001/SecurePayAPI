-- Phase 2 platform foundation migration (immutable after acceptance)
-- Ownership: securepay-core platform foundation

CREATE TABLE IF NOT EXISTS platform_metadata (
    metadata_key   VARCHAR(128) PRIMARY KEY,
    metadata_value TEXT        NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

INSERT INTO platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'phase-02-executable-platform-skeleton')
ON CONFLICT (metadata_key) DO NOTHING;
