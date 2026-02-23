#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-assemble}"
case "$MODE" in
  assemble)
    TASK=":app:assembleStagingDebug"
    ;;
  install)
    TASK=":app:installStagingDebug"
    ;;
  clean-assemble)
    TASK=":app:clean :app:assembleStagingDebug"
    ;;
  *)
    echo "Usage: bash scripts/android_staging_run.sh [assemble|install|clean-assemble]" >&2
    exit 2
    ;;
esac

detect_java_major() {
  local java_bin="$1"
  "$java_bin" -version 2>&1 | awk -F'[\".]' '/version/ { if ($2 == "1") print $3; else print $2; exit }'
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
    echo "Please install JDK 17 and set JAVA_HOME." >&2
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

ensure_java17

if [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
  export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
fi

if [[ ! -d "$ANDROID_SDK_ROOT" ]]; then
  echo "ANDROID_SDK_ROOT does not exist: $ANDROID_SDK_ROOT" >&2
  echo "Install Android SDK first or set ANDROID_SDK_ROOT explicitly." >&2
  exit 2
fi

STAGING_BASE_URL_DEFAULT="https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
REGSHIELD_STAGING_BASE_URL="${REGSHIELD_STAGING_BASE_URL:-$STAGING_BASE_URL_DEFAULT}"

if [[ "$REGSHIELD_STAGING_BASE_URL" != http* ]]; then
  echo "REGSHIELD_STAGING_BASE_URL must start with http/https." >&2
  echo "Got: $REGSHIELD_STAGING_BASE_URL" >&2
  exit 2
fi

run_gradle() {
  local log_file="$1"
  shift
  ./gradlew "$@" -PREGSHIELD_STAGING_BASE_URL="$REGSHIELD_STAGING_BASE_URL" 2>&1 | tee "$log_file"
}

LOG_FILE="$(mktemp)"
trap 'rm -f "$LOG_FILE"' EXIT

if run_gradle "$LOG_FILE" $TASK; then
  exit 0
fi

# Known intermittent issues:
# 1) duplicate kapt generated files ending in " 2.java"
# 2) duplicate classes in runtime classes.jar (e.g. material R$anim.class)
if rg -q "Factory 2\\.java|hilt_aggregated_deps.* 2\\.java|already contains entry 'com/google/android/material/R\\$anim.class'|already contains entry" "$LOG_FILE"; then
  echo ""
  echo "Detected retryable duplicate build artifacts. Retrying with clean build..."
  run_gradle "$LOG_FILE" :app:clean $TASK
  exit 0
fi

echo "Gradle build failed. See log above." >&2
exit 1
