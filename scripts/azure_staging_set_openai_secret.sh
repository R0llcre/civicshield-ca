#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Deprecated: use scripts/azure_staging_enable_ask_rag.sh" >&2
exec bash "$ROOT_DIR/scripts/azure_staging_enable_ask_rag.sh"
