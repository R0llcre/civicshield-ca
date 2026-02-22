# RegShield CA AI Q&A Policy (Ask)

## Purpose
RegShield CA includes an optional AI Q&A experience ("Ask") to help users:
- Understand general California/U.S. immigration concepts in plain language
- Find appropriate legal aid and official resources
- Follow safer next steps (credential verification, official links, documentation)

## Not Legal Advice
- The Ask feature provides **general information only**.
- It is **not legal advice** and does not create an attorney-client relationship.
- Users should consult a qualified attorney or accredited representative for case-specific guidance.

## Sensitive Information Warning
Ask must warn users not to enter sensitive personal data, including (non-exhaustive):
- A-number, passport/ID numbers
- Full home address
- Any other identifiers that could be used to uniquely identify a person

## Data Handling & Storage
- Client-side chat history is stored **locally only**:
  - Web: `localStorage`
  - Android: DataStore (local on device)
- Server does **not** store conversations in RegShield databases.
- The server proxies requests to OpenAI using `OPENAI_API_KEY` and sets `store: false` for the OpenAI request.
- Server logs must not include message contents. Log only coarse metadata (request id, status, latency).

## Sources & Citations
- Ask responses may include citations.
- Citations are filtered server-side using an allowlist of official/legal-aid domains (e.g., `uscis.gov`, `justice.gov`, `ca.gov`, `calbar.ca.gov`).
- If a citation is not allowlisted, it is dropped from the response.

## Abuse & Rate Limits
- The `/api/ask` endpoint is rate-limited per client IP.
- If rate-limited, return HTTP `429` with a short retry hint.

## UX Requirements
- Disclaimer must be visible on the Ask page and cannot be hidden.
- First use requires a lightweight confirmation ("I understand") before sending requests.
- Provide a one-click "Clear chat" action to remove local history.

