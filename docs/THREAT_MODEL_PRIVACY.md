# Threat Model & Privacy Notes (MVP)

## Data Classification

- Public roster data (DOJ PDFs): public, non-sensitive.
- Evidence vault content (uploads, screenshots, receipts): potentially sensitive.

## Privacy Commitments

- Evidence vault is stored **locally in the browser** (IndexedDB).
- Evidence export is an encrypted zip; import requires passphrase; passphrase is never sent to server.
- The app does not upload evidence by default.

## Cryptography (Evidence Vault)

- KDF: PBKDF2(SHA-256), default `210,000` iterations, random 16-byte salt.
- Encryption: AES-256-GCM with random 12-byte IV.
- Payload encoding: a single encrypted payload with a 1-byte kind prefix (`0x01` = JSON) to support future formats.
- Export format intentionally encrypts **one payload** to avoid leaking item counts / filenames on wrong passphrase.
- The exported zip contains only:
  - `vault_v1.json` (KDF params, no filenames or counts)
  - `vault_payload.enc` (encrypted blob)

## Logging Rules

- Do not log:
  - evidence content
  - filenames
  - document identifiers
  - addresses / IDs entered by users

Notes:
- The Verify UI uses `POST /api/verify/ra` so names are not placed into URL query strings by default.
- If Sentry is enabled, it is configured to avoid default PII and to strip query strings and request bodies.

## Residual Risks

- Browser storage compromise (malware, shared devices).
  - Mitigation: vault locked by passphrase; export required for backup.
- Incorrect roster parsing.
  - Mitigation: evidence links + disclaimers; import-run tracking; parser tests.
