# Engineering Checklist (Must Run Every Change)

This file is a hard requirement for this repo:

- Every change must be **verified** end-to-end.
- Every change must produce a new entry under `docs/changes/` explaining **why**, **what**, and **how we verified**.
- Never paste secrets (OpenAI/Azure/etc.) into chat, PRs, issues, or shell history.
  - Use local files under `.tmp/` (gitignored) for secrets.

## 0) Preconditions

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
git status --porcelain=v1
```

Expected: clean working tree (or only expected local `.tmp/` files).

## 1) Web (Local) - Always

```bash
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

Optional (heavier, run if UI changed):

```bash
npm -w apps/web run test:e2e
```

## 2) RAG KB + Vector Store Sync (Ask changes)

If Ask/RAG behavior or KB changed:

1) Ensure OpenAI key is stored locally (never in env/history):

```bash
mkdir -p .tmp
$EDITOR .tmp/openai_api_key.txt
```

2) Sync KB to OpenAI Vector Store:

```bash
node scripts/rag_sync_openai_vector_store.mjs
```

Outputs:
- `.tmp/openai_vector_store_id.txt` (vector store id)

## 3) Azure Staging Deploy (Ask/backend changes)

1) Build/push image to ACR and deploy to ACA:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
bash scripts/azure_staging_deploy_web.sh
```

2) Configure Ask RAG env/secrets on Azure staging (OpenAI key + vector store id):

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
bash scripts/azure_staging_enable_ask_rag.sh
```

3) Smoke test staging:

```bash
node scripts/smoke_api.mjs --base-url "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io" --max-ra-age-hours 48
curl -fsS -X POST \
  "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io/api/ask" \
  -H "content-type: application/json" \
  -d '{"locale":"en","messages":[{"role":"user","content":"How do I verify an accredited representative in California?"}]}'
```

Expected:
- `/api/meta/health` ok (postgres)
- `/api/ask` returns JSON with `answer_markdown` and allowlisted `citations`, or a strict refusal template.

Rollback (staging):

```bash
az containerapp revision list -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg -o table
# then activate the previous revision
az containerapp revision activate -g rg-regshieldca-stg-westus2 -n ca-regshieldca-web-stg --revision <REVISION_NAME>
```

## 4) Android (Staging) - Always

Start/reuse full-screen emulator, install, and launch:

```bash
cd "/Users/shenkairan/Documents/RegShield CA"
npm run android:staging:up
```

Manual demo script (must pass):
1) Verify: search `Diaz` (or any query) -> open official source link.
2) Shield: toggle red flags, open Vault.
3) Ask: ask a CA verification question -> show citations + suggested actions.
4) Help: filter by county -> see results.

## 5) Change Log Entry (Required)

Create a new file:
- `docs/changes/YYYY-MM-DD_<topic>.md`

Use the template in:
- `docs/changes/TEMPLATE.md`

It must include:
- Why
- What changed (paths)
- Verification commands run + results
- How to demo (Android + web)
- Rollback steps

