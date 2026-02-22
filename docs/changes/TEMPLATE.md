# Change Log Template

Date:

Owner:

Topic:

## Why

- What problem are we solving?
- What tradeoffs did we choose and why?

## What Changed

- Bullet list of code/doc changes with absolute paths.
- Note any behavior changes, env var changes, or backwards-compat considerations.

## How To Verify (Commands + Results)

Include:
- Commands executed (copy/paste exact commands)
- Results (pass/fail, key outputs)

Example:

```bash
npm -w apps/web run lint
npm -w apps/web run typecheck
npm -w apps/web run test
```

## How To Demo (Step-by-step)

Web:
- URL(s) to open
- Steps

Android:
- `npm run android:staging:up`
- What to click / expected output

## Rollback / Recovery

Web (Azure Container Apps):
- how to activate a previous revision

Ask (OpenAI):
- how to disable Ask quickly (unset env var)

## Notes

- Any follow-ups, risks, or TODOs.

