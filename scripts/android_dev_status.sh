#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.tmp/regshield-dev"

REGSHIELD_ANDROID_AVD="${REGSHIELD_ANDROID_AVD:-Pixel_3a_API_34_extension_level_7_arm64-v8a}"
REGSHIELD_WEB_PORT="${REGSHIELD_WEB_PORT:-3000}"
WEB_URL="http://127.0.0.1:${REGSHIELD_WEB_PORT}"

WEB_PID_FILE="$STATE_DIR/web.pid"
WEB_STARTED_BY_SCRIPT_FILE="$STATE_DIR/web.started_by_script"
WEB_LOG_FILE="$STATE_DIR/web.log"
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

web_ready() {
  curl -fsS --max-time 2 "${WEB_URL}/api/meta/health" >/dev/null 2>&1 || curl -fsS --max-time 2 "${WEB_URL}/" >/dev/null 2>&1
}

echo "RegShield Android dev status"
echo "Repo: $ROOT_DIR"
echo "State dir: $STATE_DIR"
echo "Default AVD: $REGSHIELD_ANDROID_AVD"
echo "Web URL: $WEB_URL"
echo ""

if [[ -n "$ADB_CMD" ]]; then
  echo "adb devices:"
  "$ADB_CMD" devices || true
else
  echo "adb: not found"
fi

echo ""
if web_ready; then
  echo "Web reachable: yes"
else
  echo "Web reachable: no"
fi

echo ""
if [[ -f "$WEB_STARTED_BY_SCRIPT_FILE" ]]; then
  echo "Web ownership: started by script"
else
  echo "Web ownership: external or unknown"
fi
if [[ -f "$WEB_PID_FILE" ]]; then
  echo "Web PID file: $(cat "$WEB_PID_FILE" 2>/dev/null || true)"
else
  echo "Web PID file: (none)"
fi

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

echo "Web log: $WEB_LOG_FILE"
echo "Emulator log: $EMU_LOG_FILE"
