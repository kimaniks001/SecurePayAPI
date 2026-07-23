-- Phase 14 authentication credential foundation
-- Ownership: authentication domain
--
-- Password hashes are authentication secrets and must never be returned by
-- public APIs, written to logs, or copied into audit/event payloads.

CREATE SCHEMA IF NOT EXISTS authentication;

COMMENT ON SCHEMA authentication IS
    'Authentication domain — credentials used to verify SecurePay identities';

CREATE TABLE IF NOT EXISTS authentication.authentication_credentials (
    identity_id         UUID PRIMARY KEY
                        REFERENCES identity.ks_identities (id),
    password_hash       VARCHAR(255) NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deactivated_at      TIMESTAMPTZ,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT authentication_credentials_password_hash_not_blank
        CHECK (LENGTH(TRIM(password_hash)) > 0),

    CONSTRAINT authentication_credentials_version_non_negative
        CHECK (version >= 0),

    CONSTRAINT authentication_credentials_deactivated_when_inactive
        CHECK (
            (active = TRUE AND deactivated_at IS NULL)
            OR
            (active = FALSE AND deactivated_at IS NOT NULL)
        ),

    CONSTRAINT authentication_credentials_deactivated_after_created
        CHECK (
            deactivated_at IS NULL
            OR deactivated_at >= created_at
        ),

    CONSTRAINT authentication_credentials_updated_after_created
        CHECK (updated_at >= created_at)
);

CREATE INDEX IF NOT EXISTS idx_authentication_credentials_active
    ON authentication.authentication_credentials (active);

COMMENT ON TABLE authentication.authentication_credentials IS
    'One password credential per KS identity. Password hashes are confidential authentication data.';

COMMENT ON COLUMN authentication.authentication_credentials.password_hash IS
    'One-way encoded password hash. Never expose through APIs, logs, audit records, or domain events.';
