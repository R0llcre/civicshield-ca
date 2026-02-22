#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="${ROOT_DIR}/apps/ios/CivicShield"
DERIVED_DIR="${ROOT_DIR}/.tmp/ios-derived"
LOG_DIR="${ROOT_DIR}/.tmp/ios"

DEVICE_NAME="${CIVICSHIELD_IOS_DEVICE:-iPhone 15 Pro Max}"
SCHEME="CivicShield-Staging"
BUNDLE_ID="org.regshield.ca"

mkdir -p "${LOG_DIR}"

if ! command -v xcodegen >/dev/null 2>&1; then
  echo "xcodegen is required. Install with: brew install xcodegen" >&2
  exit 1
fi

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild is required (install Xcode from the App Store)." >&2
  exit 1
fi

if ! command -v xcrun >/dev/null 2>&1; then
  echo "xcrun is required (install Xcode CLT)." >&2
  exit 1
fi

echo "Generating Xcode project (XcodeGen)..."
cd "${IOS_DIR}"
xcodegen generate > "${LOG_DIR}/xcodegen.log" 2>&1

echo "Selecting iOS Simulator device: ${DEVICE_NAME}"
UDID="$(
  python3 - "${DEVICE_NAME}" <<'PY'
import json
import subprocess
import sys

device_name = sys.argv[1]
j = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "-j", "devices", "available"]))

def parse_runtime_version(runtime_id: str):
  # "com.apple.CoreSimulator.SimRuntime.iOS-18-2" -> (18, 2)
  if ".iOS-" not in runtime_id:
    return None
  suffix = runtime_id.split(".iOS-")[-1]
  parts = suffix.split("-")
  out = []
  for p in parts:
    try:
      out.append(int(p))
    except ValueError:
      return None
  return tuple(out)

candidates = []
for runtime_id, devices in j.get("devices", {}).items():
  ver = parse_runtime_version(runtime_id)
  if not ver:
    continue
  for d in devices:
    if not d.get("isAvailable"):
      continue
    if d.get("name") != device_name:
      continue
    candidates.append((ver, d.get("udid")))

if not candidates:
  # Print a helpful message listing iOS device names.
  names = set()
  for runtime_id, devices in j.get("devices", {}).items():
    if parse_runtime_version(runtime_id):
      for d in devices:
        if d.get("isAvailable"):
          names.add(d.get("name", ""))
  names = sorted(n for n in names if n)
  print("ERROR: A matching simulator device was not found.", file=sys.stderr)
  print("Available iOS devices:", file=sys.stderr)
  for n in names:
    print(f"- {n}", file=sys.stderr)
  sys.exit(2)

candidates.sort(key=lambda x: x[0], reverse=True)
print(candidates[0][1])
PY
)"

echo "Booting simulator (udid=${UDID})..."
open -a Simulator >/dev/null 2>&1 || true
xcrun simctl boot "${UDID}" >/dev/null 2>&1 || true
xcrun simctl bootstatus "${UDID}" -b

echo "Building (${SCHEME})..."
mkdir -p "${DERIVED_DIR}"
xcodebuild build \
  -project CivicShield.xcodeproj \
  -scheme "${SCHEME}" \
  -destination "platform=iOS Simulator,id=${UDID}" \
  -derivedDataPath "${DERIVED_DIR}" \
  CODE_SIGNING_ALLOWED=NO \
  > "${LOG_DIR}/build.log" 2>&1

APP_PATH="$(find "${DERIVED_DIR}/Build/Products" -type d -name "CivicShield.app" | head -n 1 || true)"
if [[ -z "${APP_PATH}" ]]; then
  echo "Failed to find built app under ${DERIVED_DIR}/Build/Products" >&2
  echo "See log: ${LOG_DIR}/build.log" >&2
  exit 1
fi

echo "Installing app: ${APP_PATH}"
xcrun simctl install "${UDID}" "${APP_PATH}"

echo "Launching: ${BUNDLE_ID}"
xcrun simctl launch "${UDID}" "${BUNDLE_ID}" >/dev/null

echo ""
echo "CivicShield iOS staging is ready."
echo "Device: ${DEVICE_NAME}"
echo "UDID: ${UDID}"
echo "Logs:"
echo "- ${LOG_DIR}/xcodegen.log"
echo "- ${LOG_DIR}/build.log"
