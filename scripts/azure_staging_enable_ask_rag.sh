#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

AZURE_RG_DEFAULT="rg-regshieldca-stg-westus2"
AZURE_APP_DEFAULT="ca-regshieldca-web-stg"
AZURE_FQDN_DEFAULT="ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
OPENAI_MODEL_DEFAULT="gpt-4.1"

AZURE_RG="${AZURE_RG:-$AZURE_RG_DEFAULT}"
AZURE_APP="${AZURE_APP:-$AZURE_APP_DEFAULT}"
AZURE_FQDN="${AZURE_FQDN:-$AZURE_FQDN_DEFAULT}"
OPENAI_MODEL="${OPENAI_MODEL:-$OPENAI_MODEL_DEFAULT}"

KEY_FILE_DEFAULT="$ROOT_DIR/.tmp/openai_api_key.txt"
VECTOR_STORE_ID_FILE_DEFAULT="$ROOT_DIR/.tmp/openai_vector_store_id.txt"

OPENAI_KEY_FILE="${OPENAI_KEY_FILE:-$KEY_FILE_DEFAULT}"
OPENAI_VECTOR_STORE_ID_FILE="${OPENAI_VECTOR_STORE_ID_FILE:-$VECTOR_STORE_ID_FILE_DEFAULT}"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI (az) is required. Install it and retry." >&2
  exit 2
fi

if [[ ! -f "$OPENAI_KEY_FILE" ]]; then
  echo "Missing OpenAI key file: $OPENAI_KEY_FILE" >&2
  echo "Create a NEW key and store it there (one line), then retry." >&2
  exit 2
fi

if [[ ! -f "$OPENAI_VECTOR_STORE_ID_FILE" ]]; then
  echo "Missing vector store id file: $OPENAI_VECTOR_STORE_ID_FILE" >&2
  echo "Run: node scripts/rag_sync_openai_vector_store.mjs" >&2
  exit 2
fi

OPENAI_KEY="$(tr -d '\r\n' <"$OPENAI_KEY_FILE" || true)"
if [[ -z "$OPENAI_KEY" || "$OPENAI_KEY" != sk-* ]]; then
  echo "OpenAI key file does not look valid: $OPENAI_KEY_FILE" >&2
  exit 2
fi

VECTOR_STORE_ID="$(tr -d '\r\n' <"$OPENAI_VECTOR_STORE_ID_FILE" || true)"
if [[ -z "$VECTOR_STORE_ID" || "$VECTOR_STORE_ID" != vs_* ]]; then
  echo "Vector store id file does not look valid: $OPENAI_VECTOR_STORE_ID_FILE" >&2
  exit 2
fi

echo "Setting OpenAI secret on Azure Container App (no output of secret)..."
az containerapp secret set -g "$AZURE_RG" -n "$AZURE_APP" \
  --secrets "openai-api-key=$OPENAI_KEY" >/dev/null

echo "Setting Ask RAG env vars..."
az containerapp update -g "$AZURE_RG" -n "$AZURE_APP" \
  --set-env-vars \
    OPENAI_API_KEY=secretref:openai-api-key \
    OPENAI_MODEL="$OPENAI_MODEL" \
    OPENAI_VECTOR_STORE_ID="$VECTOR_STORE_ID" \
    OPENAI_ENABLE_WEB_SEARCH=true \
    OPENAI_WEB_SEARCH_EXTERNAL=false \
    OPENAI_RAG_STRICT=true \
  >/dev/null

REVISION="$(az containerapp show -g "$AZURE_RG" -n "$AZURE_APP" --query properties.latestRevisionName -o tsv)"
if [[ -n "$REVISION" ]]; then
  echo "Restarting revision to apply secret changes: $REVISION"
  az containerapp revision restart -g "$AZURE_RG" -n "$AZURE_APP" --revision "$REVISION" >/dev/null
fi

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

echo "Ask RAG is enabled on staging."
