#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.tmp/regshield-staging"

REGSHIELD_ANDROID_AVD="${REGSHIELD_ANDROID_AVD:-Pixel_8_Pro_API_34}"
STAGING_BASE_URL_DEFAULT="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
REGSHIELD_STAGING_BASE_URL="${REGSHIELD_STAGING_BASE_URL:-$STAGING_BASE_URL_DEFAULT}"

EMU_SERIAL_FILE="$STATE_DIR/emulator.serial"
EMU_STARTED_BY_SCRIPT_FILE="$STATE_DIR/emulator.started_by_script"
EMU_LOG_FILE="$STATE_DIR/emulator.log"

if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
  ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
fi

if [[ -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]]; then
  ADB_CMD="$ANDROID_SDK_ROOT/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
  ADB_CMD="$(command -v adb)"
else
  ADB_CMD=""
fi

echo "RegShield Android staging status"
echo "Repo: $ROOT_DIR"
echo "State dir: $STATE_DIR"
echo "Default AVD: $REGSHIELD_ANDROID_AVD"
echo "API base: $REGSHIELD_STAGING_BASE_URL"
echo ""

if [[ -n "$ADB_CMD" ]]; then
  echo "adb devices:"
  "$ADB_CMD" devices || true
else
  echo "adb: not found"
fi

echo ""
if curl -fsS --max-time 3 "${REGSHIELD_STAGING_BASE_URL%/}/api/meta/health" >/dev/null 2>&1; then
  echo "Staging reachable: yes"
else
  echo "Staging reachable: no"
fi

echo ""
if [[ -f "$EMU_STARTED_BY_SCRIPT_FILE" ]]; then
  if [[ -f "$EMU_SERIAL_FILE" ]]; then
    echo "Emulator ownership: started by script"
  else
    echo "Emulator ownership: stale marker (started_by_script with no serial)"
  fi
else
  echo "Emulator ownership: external or unknown"
fi

if [[ -f "$EMU_SERIAL_FILE" ]]; then
  echo "Emulator serial file: $(cat "$EMU_SERIAL_FILE" 2>/dev/null || true)"
else
  echo "Emulator serial file: (none)"
fi

echo "Emulator log: $EMU_LOG_FILE"

