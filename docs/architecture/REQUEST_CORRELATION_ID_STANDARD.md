# Request and Correlation ID Standard

**Status:** Locked doctrine (Phase 2 implementation)
**Applies to:** All SecurePay HTTP services

## Purpose

Every HTTP request must be traceable across services, logs, and error responses without exposing sensitive data.

## Headers

| Header | Required | Behaviour |
| --- | --- | --- |
| `X-Request-Id` | Response always | Unique per HTTP request. Generated server-side if absent or invalid. |
| `X-Correlation-Id` | Response always | Correlates a business operation across calls. Accepted from client when valid; generated if absent. |

**Aliases accepted for compatibility:** `X-Request-ID`, `X-Correlation-ID` (case-insensitive header names per HTTP).

## Format rules

| Rule | Value |
| --- | --- |
| Minimum length | 8 characters |
| Maximum length | 128 characters |
| Allowed characters | `A–Z`, `a–z`, `0–9`, `.`, `_`, `-` |
| Must start with | Alphanumeric character |
| Server-generated request ID prefix | `req_` |
| Server-generated correlation ID prefix | `corr_` |

Invalid correlation IDs return `400` with error code `INVALID_CORRELATION_ID`. Invalid request IDs are replaced with a new server-generated value.

## Response and error metadata

Both IDs are returned in:

- Response headers (`X-Request-Id`, `X-Correlation-Id`)
- `meta.request_id` and `meta.correlation_id` in success and error envelopes
- Structured JSON application logs (MDC fields `request_id`, `correlation_id`)

## Trust boundary

Clients may supply correlation IDs for cross-service tracing. Request IDs should be generated or validated server-side. **Do not trust unbounded or unsanitized header values.**

## Phase 2 scope

Implemented in `shared/platform-web` via `RequestCorrelationFilter`.

## Related documents

- [OpenAPI foundation](../../contracts/openapi/securepay-api-v1.yaml)
- [Logging and Redaction Standard](../security/LOGGING_AND_REDACTION_STANDARD.md)
