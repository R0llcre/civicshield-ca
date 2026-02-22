# naughty-davinci merge safety fixes

## Why
- This worktree introduced regressions that were risky to merge:
  - `/api/meta/health` removed compatibility fields still consumed by scripts/mobile models.
  - `unknown` IPs were forced into a shared rate-limit bucket, causing avoidable 429s.
  - CSP was switched from report-only to enforced without rollout safety.

## What changed
- Restored backward-compatible health fields in:
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/apps/web/src/app/api/meta/health/route.ts`
  - Fields restored across `postgres`/`sample`/`supabase`: `supabase_configured`, `db_configured`, `ask_configured`, `rag_configured`, `web_search_enabled`, `web_search_external`, `rag_strict`.
  - Kept `error` plus `db_error` details in postgres failure mode.
- Added regression tests for health response compatibility:
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/apps/web/src/app/api/meta/health/route.test.ts`
- Restored `unknown` IP bypass for rate limiting:
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/apps/web/src/app/api/verify/ra/route.ts`
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/apps/web/src/app/api/directory/search/route.ts`
- Switched CSP header back to rollout-safe report-only mode:
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/apps/web/next.config.ts`
- Added README note on CSP rollout posture:
  - `/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci/README.md`

## How to verify
```bash
cd "/Users/shenkairan/Documents/Spring26/RegShield CA/.claude/worktrees/naughty-davinci"
npm -w apps/web run test
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run build
node scripts/smoke_api.mjs --base-url "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"
```

Observed results:
- `test/lint/typecheck/build` passed.
- Smoke without strict Supabase requirement passed:
  - `[OK] health ok (supabase=false)`
  - `[OK] verify ok (...)`
  - `[OK] directory ok (...)`

Note:
- `--require-supabase` currently fails in this staging because it is running with `supabase_configured=false` by environment design, not due to missing health fields.

## How to demo
1. Start app locally or deploy staging build from this branch.
2. Call:
   - `GET /api/meta/health` and verify compatibility fields are present.
   - `GET /api/verify/ra?name=Diaz&state=CA` with and without proxy IP headers to confirm no shared `unknown` bucket throttling.
3. Inspect response headers and confirm CSP is report-only.

## Rollback
- Revert this branch commit (single revert commit).
- If already merged, revert the merge commit in `main` and redeploy previous revision.
