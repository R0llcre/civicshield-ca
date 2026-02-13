# RegShield CA (Android)

This is the **public-side** Android app (Verify / Shield + Evidence Vault / Help). Partner portal remains on Web.

## Build Variants

- Product flavors: `dev`, `staging`, `prod`
- Example tasks:
  - `./gradlew :app:assembleDevDebug`
  - `./gradlew :app:assembleStagingDebug`

`BuildConfig.API_BASE_URL` is defined per flavor in `apps/android/app/build.gradle.kts`.

You can override base URLs at build time:

- `-PREGSHIELD_STAGING_BASE_URL="https://<vercel-staging-host>"`
- `-PREGSHIELD_PROD_BASE_URL="https://<prod-host>"`

## Local Dev (Emulator)

One command startup:

```bash
npm run android:dev:up
```

This command will:
- ensure JDK 17
- start/reuse emulator (default AVD strategy: `Pixel_8_Pro_API_34`, fallback match `Pixel_8_Pro*API_34`)
- start/reuse local web server (`npm run dev`)
- install and launch `devDebug`
- verify target-device install by AVD serial (prevents accidental install to the wrong emulator)

Control commands:

```bash
npm run android:dev:status
npm run android:dev:down
```

Optional environment overrides:
- `REGSHIELD_ANDROID_AVD` (default `Pixel_8_Pro_API_34`; can be any available AVD)
- `REGSHIELD_WEB_PORT` (default `3000`)
- `REGSHIELD_ANDROID_BOOT_TIMEOUT_SEC` (default `240`)

Known failure recovery:
- AVD not found: run `"$HOME/Library/Android/sdk/emulator/emulator" -list-avds`, then export `REGSHIELD_ANDROID_AVD`
- JDK 17 missing: install JDK 17 and rerun
- SDK tools missing: install Android SDK Platform-Tools + Emulator
- kapt duplicate generated files (`*Factory 2.java`): run `npm run android:dev:clean-assemble`

Recommended full-screen test device:
- Pixel 8 Pro (API 34), gesture navigation enabled
- Also validate one smaller phone profile (for example Pixel 3a) for density regression

Fullscreen UI acceptance checklist:
- no oversized top whitespace on Verify/Shield/Help/Vault first screen
- floating bottom capsule does not overlap gesture bar
- last list card can scroll above bottom navigation with visible spacing
- portrait/landscape both keep icons and labels fully visible

## Staging APK (Internal)

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug
```

Override staging base URL (recommended):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingDebug -PREGSHIELD_STAGING_BASE_URL="https://<vercel-staging-host>"
```

Output:

- `apps/android/app/build/outputs/apk/staging/debug/app-staging-debug.apk`

## Internal Signed Release (Upgradeable)

By default, `release` builds fall back to the debug signing key for local convenience.
For internal distribution (where upgrades must install cleanly), inject a stable signing key via env:

- `REGSHIELD_INTERNAL_KEYSTORE_PATH`
- `REGSHIELD_INTERNAL_KEYSTORE_PASSWORD`
- `REGSHIELD_INTERNAL_KEY_ALIAS`
- `REGSHIELD_INTERNAL_KEY_PASSWORD`

You can also override the version for internal builds:

- `-PREGSHIELD_VERSION_CODE=123` (must be monotonically increasing per package)
- `-PREGSHIELD_VERSION_NAME="2026.02.10+123"`

Example (local):

```bash
REGSHIELD_INTERNAL_KEYSTORE_PATH="/path/to/internal.keystore" \
REGSHIELD_INTERNAL_KEYSTORE_PASSWORD="..." \
REGSHIELD_INTERNAL_KEY_ALIAS="..." \
REGSHIELD_INTERNAL_KEY_PASSWORD="..." \
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:assembleStagingRelease \
  -PREGSHIELD_STAGING_BASE_URL="https://<vercel-staging-host>" \
  -PREGSHIELD_VERSION_CODE=123 \
  -PREGSHIELD_VERSION_NAME="2026.02.10+123"
```

In CI, use `.github/workflows/android-internal-release.yml`.

## Evidence Vault Compatibility

The vault ZIP format and crypto are designed to match the Web MVP:

- PBKDF2-HMAC-SHA256, `iterations=210_000`, 16-byte salt (base64)
- AES-256-GCM (12-byte IV)
- Zip entries:
  - `vault_v1.json`
  - `vault_payload.enc`

The export encrypts a single payload to avoid leaking filenames/counts on wrong passphrase.
