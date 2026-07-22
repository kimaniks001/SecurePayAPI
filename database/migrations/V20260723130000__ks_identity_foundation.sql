-- Phase 4 KS identity foundation migration (immutable after acceptance)
-- Ownership: identity domain (KS Number issuance and aliases)

CREATE SCHEMA IF NOT EXISTS identity;

COMMENT ON SCHEMA identity IS 'KS identity domain — canonical KS Number issuance, lifecycle, and aliases';

INSERT INTO platform.platform_metadata (metadata_key, metadata_value)
VALUES ('platform_phase', 'phase-04-ksnumber-identity-issuance')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = (NOW() AT TIME ZONE 'UTC');

-- Canonical sequence allocator. Gaps may appear after rollback or failed transactions.
CREATE SEQUENCE IF NOT EXISTS identity.ks_number_sequence
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;

COMMENT ON SEQUENCE identity.ks_number_sequence IS
    'Atomic canonical KS Number sequence allocator. Gaps are acceptable; numbers are never reused.';

CREATE TABLE IF NOT EXISTS identity.ks_identities (
    id                      UUID PRIMARY KEY,
    canonical_ks_number     VARCHAR(32)  NOT NULL,
    sequence_number         BIGINT       NOT NULL,
    identity_type           VARCHAR(32)  NOT NULL,
    status                  VARCHAR(32)  NOT NULL,
    display_name            VARCHAR(128),
    issuance_request_key    VARCHAR(128) NOT NULL,
    issuance_request_hash   VARCHAR(128) NOT NULL,
    created_by_actor_type   VARCHAR(64)  NOT NULL,
    created_by_actor_id     VARCHAR(128),
    request_id              VARCHAR(128) NOT NULL,
    correlation_id          VARCHAR(128) NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    suspended_at            TIMESTAMPTZ,
    closed_at               TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ks_identities_canonical_ks_number_unique UNIQUE (canonical_ks_number),
    CONSTRAINT ks_identities_sequence_number_unique UNIQUE (sequence_number),
    CONSTRAINT ks_identities_issuance_request_key_unique UNIQUE (issuance_request_key),
    CONSTRAINT ks_identities_sequence_positive CHECK (sequence_number > 0),
    CONSTRAINT ks_identities_version_non_negative CHECK (version >= 0),
    CONSTRAINT ks_identities_type_check CHECK (identity_type IN ('INDIVIDUAL', 'SYSTEM', 'TEST')),
    CONSTRAINT ks_identities_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT ks_identities_canonical_format CHECK (canonical_ks_number ~ '^KS[0-9]{3,}$'),
    CONSTRAINT ks_identities_suspended_after_created CHECK (
        suspended_at IS NULL OR suspended_at >= created_at
    ),
    CONSTRAINT ks_identities_closed_after_created CHECK (
        closed_at IS NULL OR closed_at >= created_at
    ),
    CONSTRAINT ks_identities_updated_after_created CHECK (updated_at >= created_at)
);

CREATE INDEX IF NOT EXISTS idx_ks_identities_status ON identity.ks_identities (status);
CREATE INDEX IF NOT EXISTS idx_ks_identities_identity_type ON identity.ks_identities (identity_type);
CREATE INDEX IF NOT EXISTS idx_ks_identities_correlation_id ON identity.ks_identities (correlation_id);

CREATE TABLE IF NOT EXISTS identity.ks_number_aliases (
    id                          UUID PRIMARY KEY,
    identity_id                 UUID         NOT NULL REFERENCES identity.ks_identities (id),
    alias                       VARCHAR(64)  NOT NULL,
    normalized_alias            VARCHAR(64)  NOT NULL,
    alias_type                  VARCHAR(32)  NOT NULL,
    status                      VARCHAR(32)  NOT NULL,
    is_primary_display_alias    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by_actor_type       VARCHAR(64)  NOT NULL,
    created_by_actor_id         VARCHAR(128),
    request_id                  VARCHAR(128) NOT NULL,
    correlation_id              VARCHAR(128) NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    released_at                 TIMESTAMPTZ,
    version                     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ks_number_aliases_normalized_alias_unique UNIQUE (normalized_alias),
    CONSTRAINT ks_number_aliases_version_non_negative CHECK (version >= 0),
    CONSTRAINT ks_number_aliases_type_check CHECK (alias_type IN ('MEMORABLE', 'LEGACY', 'SYSTEM')),
    CONSTRAINT ks_number_aliases_status_check CHECK (status IN ('RESERVED', 'ACTIVE', 'SUSPENDED', 'RETIRED')),
    CONSTRAINT ks_number_aliases_not_canonical_format CHECK (normalized_alias !~ '^ks[0-9]{3,}$'),
    CONSTRAINT ks_number_aliases_released_after_created CHECK (
        released_at IS NULL OR released_at >= created_at
    ),
    CONSTRAINT ks_number_aliases_updated_after_created CHECK (updated_at >= created_at)
);

CREATE INDEX IF NOT EXISTS idx_ks_number_aliases_identity_id ON identity.ks_number_aliases (identity_id);
CREATE INDEX IF NOT EXISTS idx_ks_number_aliases_status ON identity.ks_number_aliases (status);
