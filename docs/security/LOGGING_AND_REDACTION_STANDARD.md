# Logging and Redaction Standard

**Status:** Locked doctrine (Phase 2 implementation)

## Format

Application logs use **structured JSON** (Logstash encoder) with fields including:

| Field | Description |
| --- | --- |
| `@timestamp` | UTC timestamp |
| `level` | Log level |
| `service` | Service name (e.g. `securepay-core`) |
| `environment` | Deployment environment |
| `request_id` | Per-request identifier |
| `correlation_id` | Business correlation identifier |
| `http_method` | HTTP verb |
| `http_route` | Safe route pattern (no query secrets) |
| `http_status` | Response status |
| `duration_ms` | Request duration |

## Must never log

- Passwords, OTPs, access tokens, refresh tokens
- `Authorization` headers or cookies
- Full request or response bodies
- Choice Bank payloads or credentials
- Identity documents
- Bank account numbers
- Database connection strings containing credentials
- Private keys or webhook secrets

## Redaction

`SensitiveValueRedactor` applies pattern-based redaction before diagnostic output where string concatenation is unavoidable.

## Request logging

`RequestLoggingFilter` logs one completion line per request with duration and status. It does not log bodies or sensitive headers.

## Related documents

- [Security Baseline](SECUREPAY_SECURITY_BASELINE.md)
- [Request Correlation ID Standard](../architecture/REQUEST_CORRELATION_ID_STANDARD.md)
