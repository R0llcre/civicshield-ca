# Change Log

Date: 2026-02-13

Owner: Codex (repo agent) + shenkairan

Topic: Ask RAG v1 - Structured Outputs strict schema fix + clearer staging error for model access

## Why

- Staging `/api/ask` was failing because OpenAI Structured Outputs (`strict: true`) enforces a restricted JSON schema subset:
  - For any object schema, `required` must include every key in `properties`.
  - `oneOf` is not permitted in this mode.
- We also needed a clearer error message when the configured model (`gpt-5-mini`) cannot be used due to OpenAI organization verification requirements.

Tradeoffs:
- We keep the public `/api/ask` response shape unchanged (`answer_markdown/citations/suggested_actions/disclaimer`).
- Internally, the model output schema requires `suggested_actions[].payload` to always exist. The server still strips payload for non-URL actions in the final API response, keeping client behavior stable.
- We do not add a fallback model in this change (model remains `gpt-5-mini` as requested). If the org is not verified, Ask returns a clear 503 with remediation steps.

## What Changed

- Fixed Structured Outputs schema to satisfy OpenAI strict requirements:
  - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/app/api/ask/route.ts`
    - `suggested_actions.items.required` now includes `payload` (OpenAI strict requirement).
    - `payload.additionalProperties=false` + `payload.required=["url"]` stays enforced.
- Improved OpenAI instructions formatting (use real newlines):
  - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/server/openai.ts`
- Improved error handling for model access gating (org verification):
  - `/Users/shenkairan/Documents/RegShield CA/apps/web/src/app/api/ask/route.ts`
    - Returns HTTP 503 with a clear message when OpenAI responds with "organization must be verified" for `gpt-5-*`.

## How To Verify (Commands + Results)

Web (local):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Result: PASS.

Azure staging deploy:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
bash scripts/azure_staging_deploy_web.sh
```

Result: Deployed to Azure Container Apps staging revision `ca-regshieldca-web-stg--0000009`.

Azure staging Ask RAG env/secret set:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
bash scripts/azure_staging_enable_ask_rag.sh
```

Result: secrets/env applied; Ask verification step currently returns 503 until OpenAI org is verified for `gpt-5-mini`.

## How To Demo (Step-by-step)

Web (staging):
1. Open: `https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/en/ask`
2. Ask: "How do I verify an accredited representative in California?"
3. Expected right now (until OpenAI org verification): a clear error about verifying the OpenAI organization.

After OpenAI org verification:
1. Re-run:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
bash scripts/azure_staging_enable_ask_rag.sh
```

2. Refresh Ask page and retry the same question.
3. Expected: an answer with allowlisted citations + suggested actions.

Android (staging):
1. Start full-screen emulator and install latest APK:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm run android:staging:up
```

2. In the app bottom bar, open `Ask`.
3. Send: "How do I verify an accredited representative in California?"
4. Expected right now: same "verify OpenAI organization" error.
5. After org verification + re-running `bash scripts/azure_staging_enable_ask_rag.sh`, retry:
   - Expected: answer + citations + suggested actions (Open Verify / Open Help).

## Rollback / Recovery

Web (Azure Container Apps):

```bash
az containerapp revision list -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg -o table
az containerapp revision activate -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg --revision <PREVIOUS_REVISION>
```

Ask (disable quickly):
- Unset one of these env vars on the Container App:
  - `OPENAI_API_KEY`
  - `OPENAI_VECTOR_STORE_ID`
- After that, `/api/ask` returns 501 ("not configured").

## Notes

- OpenAI org verification is required to call `gpt-5-mini` with this key. After verifying, access can take ~15 minutes to propagate.
- Vector store id used by staging is read from `.tmp/openai_vector_store_id.txt` via the enable script.

