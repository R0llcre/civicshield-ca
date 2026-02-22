#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

AZURE_RG_DEFAULT="rg-regshieldca-stg-westus2"
AZURE_APP_DEFAULT="ca-regshieldca-web-stg"
AZURE_ACR_DEFAULT="acrregshieldcastgp6ut"

AZURE_RG="${AZURE_RG:-$AZURE_RG_DEFAULT}"
AZURE_APP="${AZURE_APP:-$AZURE_APP_DEFAULT}"
AZURE_ACR="${AZURE_ACR:-$AZURE_ACR_DEFAULT}"

if ! command -v az >/dev/null 2>&1; then
  echo "Azure CLI (az) is required. Install it and retry." >&2
  exit 2
fi

if ! command -v git >/dev/null 2>&1; then
  echo "git is required. Install it and retry." >&2
  exit 2
fi

GIT_SHA="$(git rev-parse --short HEAD)"
LOGIN_SERVER="$(az acr show -n "$AZURE_ACR" --query loginServer -o tsv)"
IMAGE="${LOGIN_SERVER}/regshieldca/web:${GIT_SHA}"

echo "Building image in ACR..."
az acr build --registry "$AZURE_ACR" --image "regshieldca/web:${GIT_SHA}" --file apps/web/Dockerfile . >/dev/null

echo "Ensuring Container App can pull from ACR..."
az containerapp registry set \
  -g "$AZURE_RG" \
  -n "$AZURE_APP" \
  --server "$LOGIN_SERVER" \
  --identity system >/dev/null || true

echo "Deploying image to Container App..."
az containerapp update -g "$AZURE_RG" -n "$AZURE_APP" --image "$IMAGE" >/dev/null

FQDN="$(az containerapp show -g "$AZURE_RG" -n "$AZURE_APP" --query properties.configuration.ingress.fqdn -o tsv)"
REV="$(az containerapp show -g "$AZURE_RG" -n "$AZURE_APP" --query properties.latestRevisionName -o tsv)"

echo ""
echo "Deployed."
echo "Image: $IMAGE"
echo "Revision: $REV"
echo "FQDN: $FQDN"
echo ""
echo "Health:"
curl -fsS "https://${FQDN}/api/meta/health" || true
echo ""

