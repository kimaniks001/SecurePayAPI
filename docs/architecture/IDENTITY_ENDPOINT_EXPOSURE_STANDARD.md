# Identity Endpoint Exposure Standard

**Status:** Approved architecture standard
**Applies from:** Phase 14.5

## Purpose

Identity APIs introduced before authentication and authorization must not become accidental public administration interfaces.

## Public identity projection

Public lookup may expose only an explicitly approved projection, such as:

- canonical KS Number;
- approved public display name;
- identity type where public;
- approved public verification indicator;
- approved public profile image reference.

Public responses must not expose:

- internal identity UUID unless specifically required by an approved contract;
- phone or email;
- credential state;
- KYC data;
- bank, virtual-account or settlement mappings;
- session data;
- private lifecycle reasons;
- agreement history;
- internal risk or fraud information.

## Authenticated owner operations

Private profile, credential and account-linked information requires an authenticated actor and object-level ownership checks.

## Privileged or trusted internal operations

The following are not anonymous public operations:

- identity suspension;
- identity closure;
- reactivation;
- administrative correction;
- acting on another identity;
- credential enrolment without prior ownership proof;
- internal onboarding commands.

Until Phase 17 supplies full roles and delegated authority, privileged identity mutations must remain behind a trusted internal boundary.

## Actor rules

The API must not accept an untrusted actor identity from a request header or request body.

The temporary SYSTEM actor used during pre-authentication phases must be retired from ordinary user actions when Phase 15 introduces authenticated actor propagation.
