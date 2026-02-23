#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

LOG_DIR="$ROOT_DIR/.tmp/android-gate"
mkdir -p "$LOG_DIR"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_FILE="$LOG_DIR/nightly-full-$TIMESTAMP.log"
ln -sf "$LOG_FILE" "$LOG_DIR/nightly-latest.log"

STAGING_BASE_URL_DEFAULT="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
REGSHIELD_STAGING_BASE_URL="${REGSHIELD_STAGING_BASE_URL:-$STAGING_BASE_URL_DEFAULT}"
ANDROID_NIGHTLY_IDLE_TIMEOUT_SEC="${ANDROID_NIGHTLY_IDLE_TIMEOUT_SEC:-180}"
ANDROID_NIGHTLY_MAX_DURATION_SEC="${ANDROID_NIGHTLY_MAX_DURATION_SEC:-5400}"

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

  local resolved
  resolved="$("/usr/libexec/java_home" -v 17 2>/dev/null || true)"
  if [[ -z "$resolved" || ! -x "$resolved/bin/java" ]]; then
    echo "Could not resolve JDK 17 via /usr/libexec/java_home -v 17." >&2
    exit 2
  fi
  export JAVA_HOME="$resolved"
}

ensure_java17
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"

if [[ ! -d "$ANDROID_SDK_ROOT" ]]; then
  echo "ANDROID_SDK_ROOT does not exist: $ANDROID_SDK_ROOT" >&2
  exit 2
fi

echo "Running Android nightly full gate..."
echo "Log file: $LOG_FILE"
echo "Tasks: :app:test :app:assembleDevDebug :app:assembleStagingDebug"

python3 "$ROOT_DIR/scripts/android_gradle_watchdog.py" \
  --idle-timeout-sec "$ANDROID_NIGHTLY_IDLE_TIMEOUT_SEC" \
  --max-duration-sec "$ANDROID_NIGHTLY_MAX_DURATION_SEC" \
  --log-file "$LOG_FILE" \
  -- \
  ./gradlew --no-daemon -Dorg.gradle.workers.max=2 \
  :app:test :app:assembleDevDebug :app:assembleStagingDebug \
  -PREGSHIELD_STAGING_BASE_URL="$REGSHIELD_STAGING_BASE_URL"

echo "Android nightly full gate passed."
