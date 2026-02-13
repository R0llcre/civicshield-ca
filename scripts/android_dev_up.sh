#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.tmp/regshield-dev"
mkdir -p "$STATE_DIR"

REGSHIELD_ANDROID_AVD="${REGSHIELD_ANDROID_AVD:-Pixel_3a_API_34_extension_level_7_arm64-v8a}"
REGSHIELD_WEB_PORT="${REGSHIELD_WEB_PORT:-3000}"
REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC="${REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC:-240}"
REGSHIELD_WEB_READY_TIMEOUT_SEC="${REGSHIELD_WEB_READY_TIMEOUT_SEC:-60}"

WEB_URL="http://127.0.0.1:${REGSHIELD_WEB_PORT}"
WEB_PID_FILE="$STATE_DIR/web.pid"
WEB_STARTED_BY_SCRIPT_FILE="$STATE_DIR/web.started_by_script"
WEB_LOG_FILE="$STATE_DIR/web.log"

EMU_SERIAL_FILE="$STATE_DIR/emulator.serial"
EMU_STARTED_BY_SCRIPT_FILE="$STATE_DIR/emulator.started_by_script"
EMU_LOG_FILE="$STATE_DIR/emulator.log"

# Clean stale ownership marker left by interrupted runs.
if [[ -f "$EMU_STARTED_BY_SCRIPT_FILE" && ! -f "$EMU_SERIAL_FILE" ]]; then
  rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
fi

detect_java_major() {
  local java_bin="$1"
  "$java_bin" -version 2>&1 | awk -F'[".]' '/version/ { if ($2 == "1") print $3; else print $2; exit }'
}

ensure_java17() {
  local major=""
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    major="$(detect_java_major "${JAVA_HOME}/bin/java" || true)"
  fi

  if [[ "$major" == "17" ]]; then
    return 0
  fi

  if [[ ! -x "/usr/libexec/java_home" ]]; then
    echo "JAVA_HOME is not JDK 17 and /usr/libexec/java_home is unavailable." >&2
    echo "Install JDK 17 and set JAVA_HOME, then retry." >&2
    exit 2
  fi

  local resolved
  resolved="$("/usr/libexec/java_home" -v 17 2>/dev/null || true)"
  if [[ -z "$resolved" || ! -x "$resolved/bin/java" ]]; then
    echo "Could not resolve JDK 17 via /usr/libexec/java_home -v 17." >&2
    echo "Install JDK 17 (for example Temurin 17) and retry." >&2
    exit 2
  fi
  export JAVA_HOME="$resolved"
}

resolve_android_tools() {
  if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
    ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
  fi
  export ANDROID_SDK_ROOT

  if [[ -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]]; then
    ADB_CMD="$ANDROID_SDK_ROOT/platform-tools/adb"
  elif command -v adb >/dev/null 2>&1; then
    ADB_CMD="$(command -v adb)"
  else
    echo "Could not find adb. Expected at \$ANDROID_SDK_ROOT/platform-tools/adb or in PATH." >&2
    echo "Install Android SDK Platform-Tools and retry." >&2
    exit 2
  fi

  if [[ -x "$ANDROID_SDK_ROOT/emulator/emulator" ]]; then
    EMULATOR_CMD="$ANDROID_SDK_ROOT/emulator/emulator"
  elif command -v emulator >/dev/null 2>&1; then
    EMULATOR_CMD="$(command -v emulator)"
  else
    echo "Could not find emulator binary. Expected at \$ANDROID_SDK_ROOT/emulator/emulator or in PATH." >&2
    echo "Install Android Emulator and retry." >&2
    exit 2
  fi
}

list_avds() {
  "$EMULATOR_CMD" -list-avds 2>/dev/null || true
}

find_online_emulator_serial() {
  "$ADB_CMD" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1; exit}'
}

find_online_emulator_serial_with_retry() {
  local timeout_sec="${1:-10}"
  local deadline=$((SECONDS + timeout_sec))
  local serial=""
  while ((SECONDS < deadline)); do
    serial="$(find_online_emulator_serial)"
    if [[ -n "$serial" ]]; then
      printf "%s" "$serial"
      return 0
    fi
    sleep 2
  done
  return 1
}

is_target_avd_process_running() {
  if pgrep -f "qemu-system-aarch64.*-avd ${REGSHIELD_ANDROID_AVD}" >/dev/null 2>&1; then
    return 0
  fi
  if pgrep -f "emulator.*-avd ${REGSHIELD_ANDROID_AVD}" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

web_ready() {
  curl -fsS --max-time 2 "${WEB_URL}/api/meta/health" >/dev/null 2>&1 || curl -fsS --max-time 2 "${WEB_URL}/" >/dev/null 2>&1
}

wait_for_boot_completed() {
  local serial="$1"
  local deadline=$((SECONDS + REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC))
  "$ADB_CMD" -s "$serial" wait-for-device >/dev/null 2>&1 || true

  while ((SECONDS < deadline)); do
    local status
    status="$("$ADB_CMD" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | tr -d '\n' || true)"
    if [[ "$status" == "1" ]]; then
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for emulator boot completion after ${REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC}s." >&2
  echo "See emulator logs: $EMU_LOG_FILE" >&2
  return 1
}

wait_for_web_ready() {
  local deadline=$((SECONDS + REGSHIELD_WEB_READY_TIMEOUT_SEC))
  while ((SECONDS < deadline)); do
    if web_ready; then
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for web to be ready after ${REGSHIELD_WEB_READY_TIMEOUT_SEC}s." >&2
  echo "See web logs: $WEB_LOG_FILE" >&2
  return 1
}

ensure_java17
resolve_android_tools
"$ADB_CMD" start-server >/dev/null 2>&1 || true

AVD_LIST="$(list_avds)"
if ! printf "%s\n" "$AVD_LIST" | grep -Fxq "$REGSHIELD_ANDROID_AVD"; then
  echo "Configured AVD not found: $REGSHIELD_ANDROID_AVD" >&2
  echo "Available AVDs:" >&2
  printf "%s\n" "$AVD_LIST" >&2
  echo "Set REGSHIELD_ANDROID_AVD to one of the above and retry." >&2
  exit 2
fi

EMU_STARTED_BY_SCRIPT="no"
EMU_SERIAL="$(find_online_emulator_serial_with_retry 12 || true)"
if [[ -z "$EMU_SERIAL" ]]; then
  if is_target_avd_process_running; then
    echo "Detected running process for AVD ${REGSHIELD_ANDROID_AVD}; waiting for adb registration..."
    EMU_SERIAL="$(find_online_emulator_serial_with_retry 30 || true)"
  fi
fi

if [[ -z "$EMU_SERIAL" ]]; then
  echo "Starting emulator: $REGSHIELD_ANDROID_AVD"
  nohup "$EMULATOR_CMD" -avd "$REGSHIELD_ANDROID_AVD" >"$EMU_LOG_FILE" 2>&1 &
  EMULATOR_PID="$!"
  touch "$EMU_STARTED_BY_SCRIPT_FILE"
  EMU_STARTED_BY_SCRIPT="yes"

  local_deadline=$((SECONDS + REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC))
  while ((SECONDS < local_deadline)); do
    EMU_SERIAL="$(find_online_emulator_serial)"
    if [[ -n "$EMU_SERIAL" ]]; then
      break
    fi

    # If the emulator process exited, first try to reuse any already-online emulator.
    if ! kill -0 "$EMULATOR_PID" >/dev/null 2>&1; then
      EMU_SERIAL="$(find_online_emulator_serial_with_retry 10 || true)"
      if [[ -n "$EMU_SERIAL" ]]; then
        echo "Detected existing online emulator; reusing $EMU_SERIAL."
        rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
        EMU_STARTED_BY_SCRIPT="no"
        break
      fi
      if grep -q "Running multiple emulators with the same AVD" "$EMU_LOG_FILE"; then
        rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
        echo "Another emulator is already using AVD ${REGSHIELD_ANDROID_AVD}, but adb did not expose a device." >&2
        echo "Open Android Studio Device Manager and boot the same AVD, then rerun." >&2
        echo "See emulator logs: $EMU_LOG_FILE" >&2
        exit 1
      fi
      rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
      echo "Emulator process exited before registering in adb." >&2
      echo "See emulator logs: $EMU_LOG_FILE" >&2
      exit 1
    fi
    sleep 2
  done

  if [[ -z "$EMU_SERIAL" ]]; then
    rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
    echo "Emulator did not appear in adb devices within ${REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC}s." >&2
    echo "See emulator logs: $EMU_LOG_FILE" >&2
    exit 1
  fi

  wait_for_boot_completed "$EMU_SERIAL"
else
  rm -f "$EMU_STARTED_BY_SCRIPT_FILE"
fi
echo "$EMU_SERIAL" >"$EMU_SERIAL_FILE"

WEB_STARTED_BY_SCRIPT="no"
if web_ready; then
  rm -f "$WEB_STARTED_BY_SCRIPT_FILE" "$WEB_PID_FILE"
else
  echo "Starting local web dev server on port ${REGSHIELD_WEB_PORT}"
  nohup npm run dev >"$WEB_LOG_FILE" 2>&1 &
  WEB_PID="$!"
  echo "$WEB_PID" >"$WEB_PID_FILE"
  touch "$WEB_STARTED_BY_SCRIPT_FILE"
  WEB_STARTED_BY_SCRIPT="yes"
  wait_for_web_ready
fi

echo "Installing devDebug app..."
bash "$ROOT_DIR/scripts/android_dev_run.sh" install

echo "Launching app on emulator..."
"$ADB_CMD" -s "$EMU_SERIAL" shell monkey -p org.regshield.ca.dev -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

echo ""
echo "RegShield Android dev environment is ready."
echo "AVD: $REGSHIELD_ANDROID_AVD"
echo "Emulator serial: $EMU_SERIAL ($([[ "$EMU_STARTED_BY_SCRIPT" == "yes" ]] && echo "started by script" || echo "reused"))"
echo "Web: ${WEB_URL} ($([[ "$WEB_STARTED_BY_SCRIPT" == "yes" ]] && echo "started by script" || echo "reused"))"
echo "Web log: $WEB_LOG_FILE"
echo "Emulator log: $EMU_LOG_FILE"
echo "Stop managed services: npm run android:dev:down"
