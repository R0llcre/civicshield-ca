#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.tmp/regshield-dev"

REGSHIELD_WEB_PORT="${REGSHIELD_WEB_PORT:-3000}"
WEB_URL="http://127.0.0.1:${REGSHIELD_WEB_PORT}"
WEB_PID_FILE="$STATE_DIR/web.pid"
WEB_STARTED_BY_SCRIPT_FILE="$STATE_DIR/web.started_by_script"
EMU_SERIAL_FILE="$STATE_DIR/emulator.serial"
EMU_STARTED_BY_SCRIPT_FILE="$STATE_DIR/emulator.started_by_script"

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

echo "Stopping RegShield managed local services..."

if [[ -f "$WEB_STARTED_BY_SCRIPT_FILE" && -f "$WEB_PID_FILE" ]]; then
  WEB_PID="$(cat "$WEB_PID_FILE" 2>/dev/null || true)"
  if [[ -n "$WEB_PID" ]] && kill -0 "$WEB_PID" >/dev/null 2>&1; then
    kill "$WEB_PID" >/dev/null 2>&1 || true
    for _ in {1..10}; do
      if ! kill -0 "$WEB_PID" >/dev/null 2>&1; then
        break
      fi
      sleep 1
    done
    if kill -0 "$WEB_PID" >/dev/null 2>&1; then
      kill -9 "$WEB_PID" >/dev/null 2>&1 || true
    fi
    echo "Stopped managed web process (pid=$WEB_PID)."
  else
    echo "Managed web process already stopped."
  fi
  rm -f "$WEB_PID_FILE" "$WEB_STARTED_BY_SCRIPT_FILE"
else
  echo "Web process was not started by script; skipping."
fi

if [[ -f "$EMU_STARTED_BY_SCRIPT_FILE" ]]; then
  if [[ -f "$EMU_SERIAL_FILE" ]]; then
    EMU_SERIAL="$(cat "$EMU_SERIAL_FILE" 2>/dev/null || true)"
    if [[ -n "$EMU_SERIAL" && -n "$ADB_CMD" ]]; then
      if "$ADB_CMD" devices | awk '{print $1}' | grep -Fxq "$EMU_SERIAL"; then
        "$ADB_CMD" -s "$EMU_SERIAL" emu kill >/dev/null 2>&1 || true
        echo "Sent kill to managed emulator ($EMU_SERIAL)."
      else
        echo "Managed emulator serial not currently online ($EMU_SERIAL)."
      fi
    else
      echo "Could not locate adb or emulator serial; skipping emulator stop."
    fi
  else
    echo "Found stale emulator ownership marker without serial file; cleaning it."
  fi
  rm -f "$EMU_STARTED_BY_SCRIPT_FILE" "$EMU_SERIAL_FILE"
else
  echo "Emulator was not started by script; skipping."
fi

if [[ -n "$ADB_CMD" ]]; then
  echo ""
  echo "Remaining adb devices:"
  "$ADB_CMD" devices || true
fi

if web_ready; then
  echo "Web still reachable at $WEB_URL"
else
  echo "Web is not reachable at $WEB_URL"
fi
