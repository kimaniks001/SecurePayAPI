CREATE SCHEMA IF NOT EXISTS authentication;

CREATE TABLE IF NOT EXISTS authentication.authentication_challenges (
    id                  UUID PRIMARY KEY,
    identity_id         UUID         NOT NULL
                        REFERENCES identity.ks_identities (id),
    challenge_digest    VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    application_id      VARCHAR(128),
    device_id           VARCHAR(256),
    source_ip_hash      VARCHAR(128),
    expires_at          TIMESTAMPTZ  NOT NULL,
    consumed_at         TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL
                        DEFAULT (NOW() AT TIME ZONE 'UTC'),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT authentication_challenges_digest_unique
        UNIQUE (challenge_digest),

    CONSTRAINT authentication_challenges_status_check
        CHECK (status IN ('PENDING', 'CONSUMED', 'EXPIRED', 'REVOKED')),

    CONSTRAINT authentication_challenges_version_non_negative
        CHECK (version >= 0),

    CONSTRAINT authentication_challenges_expires_after_created
        CHECK (expires_at > created_at),

    CONSTRAINT authentication_challenges_consumed_after_created
        CHECK (consumed_at IS NULL OR consumed_at >= created_at),

    CONSTRAINT authentication_challenges_revoked_after_created
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX IF NOT EXISTS idx_authentication_challenges_identity
    ON authentication.authentication_challenges (identity_id);

CREATE INDEX IF NOT EXISTS idx_authentication_challenges_status_expiry
    ON authentication.authentication_challenges (status, expires_at);

CREATE TABLE IF NOT EXISTS authentication.authentication_sessions (
    id                  UUID PRIMARY KEY,
    identity_id         UUID         NOT NULL
                        REFERENCES identity.ks_identities (id),
    challenge_id        UUID         NOT NULL
                        REFERENCES authentication.authentication_challenges (id),
    access_token_digest VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    application_id      VARCHAR(128),
    device_id           VARCHAR(256),
    source_ip_hash      VARCHAR(128),
    authentication_method VARCHAR(64) NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL
                        DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ  NOT NULL
                        DEFAULT (NOW() AT TIME ZONE 'UTC'),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT authentication_sessions_access_digest_unique
        UNIQUE (access_token_digest),

    CONSTRAINT authentication_sessions_status_check
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'COMPROMISED')),

    CONSTRAINT authentication_sessions_version_non_negative
        CHECK (version >= 0),

    CONSTRAINT authentication_sessions_expires_after_created
        CHECK (expires_at > created_at),

    CONSTRAINT authentication_sessions_revoked_after_created
        CHECK (revoked_at IS NULL OR revoked_at >= created_at),

    CONSTRAINT authentication_sessions_updated_after_created
        CHECK (updated_at >= created_at)
);

CREATE INDEX IF NOT EXISTS idx_authentication_sessions_identity
    ON authentication.authentication_sessions (identity_id);

CREATE INDEX IF NOT EXISTS idx_authentication_sessions_status_expiry
    ON authentication.authentication_sessions (status, expires_at);

CREATE TABLE IF NOT EXISTS authentication.refresh_tokens (
    id                  UUID PRIMARY KEY,
    session_id          UUID         NOT NULL
                        REFERENCES authentication.authentication_sessions (id),
    token_digest        VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    parent_token_id     UUID
                        REFERENCES authentication.refresh_tokens (id),
    replaced_by_token_id UUID
                        REFERENCES authentication.refresh_tokens (id),
    expires_at          TIMESTAMPTZ  NOT NULL,
    rotated_at          TIMESTAMPTZ,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL
                        DEFAULT (NOW() AT TIME ZONE 'UTC'),
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT refresh_tokens_digest_unique
        UNIQUE (token_digest),

    CONSTRAINT refresh_tokens_status_check
        CHECK (status IN ('ACTIVE', 'ROTATED', 'EXPIRED', 'REVOKED', 'REPLAYED')),

    CONSTRAINT refresh_tokens_version_non_negative
        CHECK (version >= 0),

    CONSTRAINT refresh_tokens_expires_after_created
        CHECK (expires_at > created_at),

    CONSTRAINT refresh_tokens_rotated_after_created
        CHECK (rotated_at IS NULL OR rotated_at >= created_at),

    CONSTRAINT refresh_tokens_revoked_after_created
        CHECK (revoked_at IS NULL OR revoked_at >= created_at),

    CONSTRAINT refresh_tokens_replacement_pair
        CHECK (
            (status = 'ROTATED' AND rotated_at IS NOT NULL
                AND replaced_by_token_id IS NOT NULL)
            OR
            (status <> 'ROTATED')
        )
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_session
    ON authentication.refresh_tokens (session_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_status_expiry
    ON authentication.refresh_tokens (status, expires_at);
