# CivicShield (iOS)

Public client app (Verify / Shield / Ask / Help / Evidence Vault) for iOS 16+.

This project uses **XcodeGen** (`project.yml`) to generate the Xcode project.

## Quickstart (Staging, Recommended)

From repo root:

```bash
cd <repo-root>
npm run ios:staging:up
```

This will:

1) generate `CivicShield.xcodeproj` via XcodeGen  
2) boot a simulator (default: `iPhone 15 Pro Max`)  
3) build and install the app  
4) launch it in the simulator

Override the simulator device:

```bash
CIVICSHIELD_IOS_DEVICE="iPhone 15 Pro" npm run ios:staging:up
```

## Manual (Xcode)

```bash
cd apps/ios/CivicShield
xcodegen generate
open CivicShield.xcodeproj
```

Then in Xcode:

1) Select scheme `CivicShield-Staging`  
2) Pick an iOS Simulator device  
3) Run

## Run On iPhone (Device)

This repo can run on a real iPhone, but iOS requires code signing.

1) Generate and open the project:

```bash
cd apps/ios/CivicShield
xcodegen generate
open CivicShield.xcodeproj
```

2) In Xcode, select target `CivicShield` -> `Signing & Capabilities`:
- Enable `Automatically manage signing`
- Select your `Team` (Personal Team is fine)
- Signing certificate: `Apple Development`

3) If Xcode says the bundle id is not available, change it in:
- `apps/ios/CivicShield/project.yml` (`PRODUCT_BUNDLE_IDENTIFIER`)

Then re-run `xcodegen generate`.

4) Connect your iPhone via USB:
- Unlock phone and tap `Trust` when prompted
- In Xcode, pick your iPhone as the run destination and press Run

If you see `The executable is not codesigned`, it means the app was built without signing or signing is misconfigured in the selected scheme/config.

## Demo Script (What To Click)

1) **Verify**
   - Search `Diaz` or `samantha`
   - Open `Open official source`
2) **Shield**
   - Toggle 1-2 red flags + 1 action step
   - Copy/share summary
   - Open Evidence Vault
3) **Ask**
   - Ask: `How do I verify an accredited representative in California?`
   - Confirm markdown renders (lists/bold/links)
   - Open citations (allowlist enforced)
   - Use suggested actions to jump to Verify/Help
4) **Help**
   - Featured list loads on first open
   - Filter by county/language/service, then search
5) **Evidence Vault**
   - Create -> Add files -> Export zip
   - Delete vault -> Import zip -> Unlock -> items restored

Privacy: the vault is **local-only** and encrypted; nothing uploads by default.
