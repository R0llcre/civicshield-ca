#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="$ROOT_DIR/.tmp/regshield-staging"
mkdir -p "$STATE_DIR"

REGSHIELD_ANDROID_AVD_DEFAULT="Pixel_8_Pro_API_34"
REGSHIELD_ANDROID_AVD="${REGSHIELD_ANDROID_AVD:-$REGSHIELD_ANDROID_AVD_DEFAULT}"
REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC="${REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC:-240}"

STAGING_BASE_URL_DEFAULT="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
REGSHIELD_STAGING_BASE_URL="${REGSHIELD_STAGING_BASE_URL:-$STAGING_BASE_URL_DEFAULT}"

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

list_online_emulator_serials() {
  "$ADB_CMD" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1}'
}

get_avd_name_for_serial() {
  local serial="$1"
  "$ADB_CMD" -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | awk 'NF && $0 != "OK" {print; exit}'
}

find_online_serial_for_avd() {
  local target_avd="$1"
  local serial=""
  while read -r serial; do
    [[ -z "$serial" ]] && continue
    local avd_name
    avd_name="$(get_avd_name_for_serial "$serial" || true)"
    if [[ "$avd_name" == "$target_avd" ]]; then
      printf "%s" "$serial"
      return 0
    fi
  done < <(list_online_emulator_serials)
  return 1
}

find_online_serial_for_avd_with_retry() {
  local target_avd="$1"
  local timeout_sec="${2:-10}"
  local deadline=$((SECONDS + timeout_sec))
  local serial=""
  while ((SECONDS < deadline)); do
    serial="$(find_online_serial_for_avd "$target_avd" || true)"
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

assert_target_device_ready() {
  local serial="$1"
  if ! "$ADB_CMD" -s "$serial" get-state >/dev/null 2>&1; then
    echo "Target emulator serial is not online: $serial" >&2
    echo "See emulator logs: $EMU_LOG_FILE" >&2
    return 1
  fi

  local avd_name
  avd_name="$(get_avd_name_for_serial "$serial" || true)"
  if [[ -z "$avd_name" || "$avd_name" != "$REGSHIELD_ANDROID_AVD" ]]; then
    echo "Connected emulator does not match target AVD." >&2
    echo "Expected: $REGSHIELD_ANDROID_AVD" >&2
    echo "Actual: ${avd_name:-unknown}" >&2
    return 1
  fi
}

ensure_java17
resolve_android_tools
"$ADB_CMD" start-server >/dev/null 2>&1 || true

AVD_LIST="$(list_avds)"
if ! printf "%s\n" "$AVD_LIST" | grep -Fxq "$REGSHIELD_ANDROID_AVD"; then
  if [[ "$REGSHIELD_ANDROID_AVD" == "$REGSHIELD_ANDROID_AVD_DEFAULT" ]]; then
    MATCHED_AVD="$(printf "%s\n" "$AVD_LIST" | awk '/^Pixel_8_Pro.*API_34$/ {print; exit}')"
    if [[ -n "$MATCHED_AVD" ]]; then
      REGSHIELD_ANDROID_AVD="$MATCHED_AVD"
      echo "Using matched full-screen AVD: $REGSHIELD_ANDROID_AVD"
    fi
  fi
fi

if ! printf "%s\n" "$AVD_LIST" | grep -Fxq "$REGSHIELD_ANDROID_AVD"; then
  echo "Configured AVD not found: $REGSHIELD_ANDROID_AVD" >&2
  echo "Recommended full-screen AVD: Pixel_8_Pro_API_34 (or any Pixel_8_Pro*API_34)." >&2
  echo "Create one in Android Studio Device Manager and retry." >&2
  echo "Available AVDs:" >&2
  printf "%s\n" "$AVD_LIST" >&2
  echo "Set REGSHIELD_ANDROID_AVD to one of the above and retry." >&2
  exit 2
fi

EMU_STARTED_BY_SCRIPT="no"
EMU_SERIAL="$(find_online_serial_for_avd_with_retry "$REGSHIELD_ANDROID_AVD" 12 || true)"

if [[ -z "$EMU_SERIAL" ]] && is_target_avd_process_running; then
  echo "Detected running process for AVD ${REGSHIELD_ANDROID_AVD}; waiting for adb registration..."
  EMU_SERIAL="$(find_online_serial_for_avd_with_retry "$REGSHIELD_ANDROID_AVD" 30 || true)"
fi

if [[ -z "$EMU_SERIAL" ]]; then
  echo "Starting emulator: $REGSHIELD_ANDROID_AVD"
  nohup "$EMULATOR_CMD" -avd "$REGSHIELD_ANDROID_AVD" >"$EMU_LOG_FILE" 2>&1 &
  EMULATOR_PID="$!"
  touch "$EMU_STARTED_BY_SCRIPT_FILE"
  EMU_STARTED_BY_SCRIPT="yes"

  local_deadline=$((SECONDS + REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC))
  while ((SECONDS < local_deadline)); do
    EMU_SERIAL="$(find_online_serial_for_avd "$REGSHIELD_ANDROID_AVD" || true)"
    if [[ -n "$EMU_SERIAL" ]]; then
      break
    fi

    if ! kill -0 "$EMULATOR_PID" >/dev/null 2>&1; then
      EMU_SERIAL="$(find_online_serial_for_avd_with_retry "$REGSHIELD_ANDROID_AVD" 10 || true)"
      if [[ -n "$EMU_SERIAL" ]]; then
        echo "Detected existing online target AVD; reusing $EMU_SERIAL."
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
assert_target_device_ready "$EMU_SERIAL"

if [[ "$REGSHIELD_STAGING_BASE_URL" != http* ]]; then
  echo "REGSHIELD_STAGING_BASE_URL must start with http/https." >&2
  echo "Got: $REGSHIELD_STAGING_BASE_URL" >&2
  exit 2
fi

echo "Staging API base: $REGSHIELD_STAGING_BASE_URL"
if ! curl -fsS --max-time 5 "${REGSHIELD_STAGING_BASE_URL%/}/api/meta/health" >/dev/null 2>&1; then
  echo "Warning: staging host not reachable (continuing): ${REGSHIELD_STAGING_BASE_URL%/}/api/meta/health" >&2
fi

echo "Installing stagingDebug app..."
GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.workers.max=1" \
ANDROID_SERIAL="$EMU_SERIAL" REGSHIELD_STAGING_BASE_URL="$REGSHIELD_STAGING_BASE_URL" \
  bash "$ROOT_DIR/scripts/android_staging_run.sh" install

echo "Launching app on emulator..."
"$ADB_CMD" -s "$EMU_SERIAL" shell monkey -p org.regshield.ca.staging -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true

echo ""
echo "CivicShield Android staging environment is ready."
echo "AVD: $REGSHIELD_ANDROID_AVD"
echo "Emulator serial: $EMU_SERIAL ($([[ "$EMU_STARTED_BY_SCRIPT" == "yes" ]] && echo "started by script" || echo "reused"))"
echo "API base: $REGSHIELD_STAGING_BASE_URL"
echo "Emulator log: $EMU_LOG_FILE"
echo "Stop managed emulator: npm run android:staging:down"
