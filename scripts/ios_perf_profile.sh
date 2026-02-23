#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="${ROOT_DIR}/apps/ios/CivicShield"
DERIVED_DIR="${ROOT_DIR}/.tmp/ios-derived-perf"
OUT_DIR="${ROOT_DIR}/.tmp/ios-perf/$(date +%Y%m%d-%H%M%S)"

SCHEME="${CIVICSHIELD_IOS_SCHEME:-CivicShield-Staging}"
DEVICE_NAME="${CIVICSHIELD_IOS_DEVICE:-iPhone 15 Pro Max}"
DURATION_SEC="${CIVICSHIELD_IOS_PROFILE_SEC:-20}"

mkdir -p "${OUT_DIR}"

for cmd in xcodegen xcodebuild xcrun; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "$cmd is required" >&2
    exit 1
  fi
done

if ! command -v xctrace >/dev/null 2>&1; then
  echo "xctrace is required (Xcode Instruments CLI)." >&2
  exit 1
fi

echo "Generating Xcode project..."
cd "${IOS_DIR}"
xcodegen generate > "${OUT_DIR}/xcodegen.log" 2>&1

echo "Selecting simulator: ${DEVICE_NAME}"
UDID="$({
  python3 - "${DEVICE_NAME}" <<'PY'
import json
import subprocess
import sys

name = sys.argv[1]
j = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "-j", "devices", "available"]))


def parse(runtime_id: str):
    if ".iOS-" not in runtime_id:
        return None
    suffix = runtime_id.split(".iOS-")[-1]
    try:
        return tuple(int(p) for p in suffix.split("-"))
    except ValueError:
        return None

candidates = []
for runtime_id, devices in j.get("devices", {}).items():
    ver = parse(runtime_id)
    if not ver:
        continue
    for d in devices:
        if d.get("isAvailable") and d.get("name") == name:
            candidates.append((ver, d.get("udid")))

if not candidates:
    print("", end="")
    sys.exit(2)

candidates.sort(key=lambda x: x[0], reverse=True)
print(candidates[0][1])
PY
} || true)"

if [[ -z "${UDID}" ]]; then
  echo "Unable to find simulator '${DEVICE_NAME}'" >&2
  exit 1
fi

open -a Simulator >/dev/null 2>&1 || true
xcrun simctl boot "${UDID}" >/dev/null 2>&1 || true
xcrun simctl bootstatus "${UDID}" -b

mkdir -p "${DERIVED_DIR}"

echo "Building ${SCHEME}..."
xcodebuild build \
  -project CivicShield.xcodeproj \
  -scheme "${SCHEME}" \
  -destination "platform=iOS Simulator,id=${UDID}" \
  -derivedDataPath "${DERIVED_DIR}" \
  -skipMacroValidation \
  CODE_SIGNING_ALLOWED=NO \
  > "${OUT_DIR}/build.log" 2>&1

APP_PATH="$(find "${DERIVED_DIR}/Build/Products" -type d -name "CivicShield.app" | head -n1 || true)"
if [[ -z "${APP_PATH}" ]]; then
  echo "Failed to locate built app. See ${OUT_DIR}/build.log" >&2
  exit 1
fi

BUNDLE_ID="$(/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "${APP_PATH}/Info.plist")"
APP_EXECUTABLE="$(/usr/libexec/PlistBuddy -c "Print :CFBundleExecutable" "${APP_PATH}/Info.plist")"

echo "Installing ${BUNDLE_ID}..."
xcrun simctl install "${UDID}" "${APP_PATH}"

run_profile() {
  local tab="$1"
  local template="$2"
  local out="$3"

  xcrun simctl terminate "${UDID}" "${BUNDLE_ID}" >/dev/null 2>&1 || true
  xcrun simctl launch "${UDID}" "${BUNDLE_ID}" --args "--tab=${tab}" >/dev/null

  # Let the app boot before attaching profiler.
  sleep 1

  xctrace record \
    --template "${template}" \
    --output "${out}" \
    --time-limit "${DURATION_SEC}s" \
    --device "${UDID}" \
    --attach "${APP_EXECUTABLE}" \
    > /dev/null 2>&1 || true
}

echo "Recording startup profile..."
run_profile "verify" "Time Profiler" "${OUT_DIR}/startup_time_profiler.trace"

echo "Recording ask profile..."
run_profile "ask" "Time Profiler" "${OUT_DIR}/ask_time_profiler.trace"

echo "Recording vault profile..."
run_profile "shield" "Core Animation" "${OUT_DIR}/shield_core_animation.trace"

echo "iOS performance traces ready: ${OUT_DIR}"
echo "Suggested next step: open traces in Instruments and compare signposts (app_startup_task, ask_send, vault_*)."
