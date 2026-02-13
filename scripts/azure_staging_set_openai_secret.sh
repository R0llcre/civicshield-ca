#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

AZURE_RG_DEFAULT="rg-regshieldca-stg-westus2"
AZURE_APP_DEFAULT="ca-regshieldca-web-stg"
AZURE_FQDN_DEFAULT="ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
OPENAI_MODEL_DEFAULT="gpt-4o-mini"

AZURE_RG="${AZURE_RG:-$AZURE_RG_DEFAULT}"
AZURE_APP="${AZURE_APP:-$AZURE_APP_DEFAULT}"
AZURE_FQDN="${AZURE_FQDN:-$AZURE_FQDN_DEFAULT}"
OPENAI_MODEL="${OPENAI_MODEL:-$OPENAI_MODEL_DEFAULT}"

# Intentionally read the key from a local file so it never needs to be pasted into chat,
# git, or shell history.
KEY_FILE_DEFAULT="$ROOT_DIR/.tmp/openai_api_key.txt"
OPENAI_KEY_FILE="${OPENAI_KEY_FILE:-$KEY_FILE_DEFAULT}"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI (az) is required. Install it and retry." >&2
  exit 2
fi

if [[ ! -f "$OPENAI_KEY_FILE" ]]; then
  echo "Missing OpenAI key file: $OPENAI_KEY_FILE" >&2
  echo "" >&2
  echo "Create a NEW key (rotate if your key was ever pasted into chat), then save it to:" >&2
  echo "  $OPENAI_KEY_FILE" >&2
  echo "" >&2
  echo "Example:" >&2
  echo "  mkdir -p \"$(dirname "$OPENAI_KEY_FILE")\"" >&2
  echo "  (paste key into editor) > \"$OPENAI_KEY_FILE\"" >&2
  exit 2
fi

OPENAI_KEY="$(tr -d '\r\n' <"$OPENAI_KEY_FILE" || true)"
if [[ -z "$OPENAI_KEY" ]]; then
  echo "OpenAI key file is empty: $OPENAI_KEY_FILE" >&2
  exit 2
fi

if [[ "$OPENAI_KEY" != sk-* ]]; then
  echo "OpenAI key does not look like an API key (expected prefix sk-...). Refusing to continue." >&2
  exit 2
fi

echo "Setting OpenAI secret on Azure Container App..."
az containerapp secret set -g "$AZURE_RG" -n "$AZURE_APP" \
  --secrets "openai-api-key=$OPENAI_KEY" >/dev/null

echo "Wiring env vars (OPENAI_API_KEY, OPENAI_MODEL)..."
az containerapp update -g "$AZURE_RG" -n "$AZURE_APP" \
  --set-env-vars \
    OPENAI_API_KEY=secretref:openai-api-key \
    OPENAI_MODEL="$OPENAI_MODEL" \
  >/dev/null

echo "Verifying /api/ask..."
ASK_URL="https://${AZURE_FQDN}/api/ask"
HTTP_CODE="$(curl -sS -o /tmp/regshield_ask_verify.json -w "%{http_code}" -X POST "$ASK_URL" \
  -H 'content-type: application/json' \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}' || true)"

if [[ "$HTTP_CODE" != "200" ]]; then
  echo "Ask check failed: HTTP $HTTP_CODE" >&2
  echo "Response body (first 400 bytes):" >&2
  head -c 400 /tmp/regshield_ask_verify.json >&2 || true
  echo "" >&2
  exit 1
fi

echo "Ask is enabled on staging."
