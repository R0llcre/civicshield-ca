# PRD (MVP) - RegShield CA

## Goal

Help users **verify** whether a person/organization appears on official rosters, reduce fraud risk with an actionable checklist, and speed up referrals to legal aid organizations.

## Target Users

- People seeking immigration legal help in California (often multilingual, mobile-first).
- Community org staff helping someone vet service providers.
- Partner organizations managing intake availability.

## Non-Goals (MVP)

- No legal advice or case strategy.
- No user account system for the public side.
- No scraping CA State Bar / CA SOS (we provide official links + education).
- No crowdsourced enforcement locations or public shaming.

## Core User Flows

### Verify

1. User enters a name (person or org) and optional state filter (default CA).
2. App searches DOJ roster data.
3. App shows matched roster entries with evidence link(s) to DOJ source PDFs.
4. App shows official manual verification links for CA State Bar and CA SOS.

### Shield

1. User reviews red flags checklist.
2. User creates/unlocks a local evidence vault with a passphrase.
3. User adds files (screenshots, receipts) to the encrypted vault.
4. User exports/imports an encrypted zip archive (local only).

### Help

1. User sets filters (county, language, service category).
2. App shows organizations and their intake status (open/waitlist/closed) and availability signals.

### Partner Portal

1. Partner user signs in with email magic link (Supabase Auth).
2. User updates organization intake status, next clinic time, callback ETA, and coverage fields.

## Compliance & Copy Requirements

- Every credential match is phrased as: **"appears on official roster"** + evidence link; no legal conclusions.
- Evidence vault is strictly **local**; no uploads by default; no sensitive data in logs.
- Clear "Information only, not legal advice" footer.

## Success Criteria (MVP)

- Verify: users can find roster matches within 1 second for typical queries (CA network).
- Shield: exported vault cannot be decrypted with wrong passphrase; export does not reveal filenames.
- Help: filters reduce irrelevant calls; partner updates reflect quickly.

