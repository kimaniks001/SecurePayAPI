#!/usr/bin/env bash
# Run all Phase 1 repository validations locally.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "=== SecurePay Phase 1 Validation Suite ==="
echo "Repository: $ROOT"
echo

failures=0

run_step() {
  local name="$1"
  shift
  echo "--- $name ---"
  if "$@"; then
    echo "OK: $name"
  else
    echo "FAILED: $name"
    failures=$((failures + 1))
  fi
  echo
}

run_step "Required files" python3 scripts/validate_required_files.py
run_step "Markdown links" python3 scripts/validate_markdown_links.py
run_step "Doctrine checks" python3 scripts/validate_doctrine.py
run_step "Secret scan" python3 scripts/scan_secrets.py

run_step "OpenAPI validation" python3 - <<'PY'
from pathlib import Path
from openapi_spec_validator import validate
from openapi_spec_validator.readers import read_from_filename

spec_path = Path("contracts/openapi/securepay-api-v1.yaml")
spec = read_from_filename(str(spec_path))[0]
validate(spec)
print(f"OpenAPI 3.1 spec valid: {spec_path}")
PY

run_step "JSON Schema validation" python3 - <<'PY'
import json
from pathlib import Path
import jsonschema

for rel in [
    "contracts/events/event-envelope-v1.schema.json",
    "contracts/errors/error-envelope-v1.schema.json",
]:
    path = Path(rel)
    schema = json.loads(path.read_text(encoding="utf-8"))
    jsonschema.Draft202012Validator.check_schema(schema)
    print(f"Valid JSON Schema: {rel}")
PY

run_step "Docker Compose config" docker compose --env-file .env.example config --quiet

echo "=== Summary ==="
if [[ "$failures" -eq 0 ]]; then
  echo "All validations PASSED."
  exit 0
else
  echo "$failures validation step(s) FAILED."
  exit 1
fi
