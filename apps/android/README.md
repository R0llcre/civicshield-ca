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

1. Start the web app:

```bash
npm run dev
```

2. Build/install the Android `devDebug` variant (emulator uses `10.0.2.2` to reach host `localhost`):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" \
./gradlew :app:installDevDebug
```

Equivalent repo scripts:

```bash
npm run android:dev:assemble
npm run android:dev:install
```

Known issue recovery (`*Factory 2.java`, `hilt_aggregated_deps ... 2.java`):

```bash
npm run android:dev:clean-assemble
```

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
