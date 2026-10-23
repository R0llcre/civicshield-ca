# Agent Instructions (RegShield CA)

This repository follows the instructions provided by the owner for Codex agents.

## Skills

A skill is a set of local instructions to follow that is stored in a `SKILL.md` file.

### Available skills

- cloudflare-deploy: Deploy applications and infrastructure to Cloudflare using Workers, Pages, and related platform services. Use when the user asks to deploy, host, publish, or set up a project on Cloudflare. (file: ~/.codex/skills/cloudflare-deploy/SKILL.md)
- doc: Use when the task involves reading, creating, or editing `.docx` documents, especially when formatting or layout fidelity matters; prefer `python-docx` plus the bundled `scripts/render_docx.py` for visual checks. (file: ~/.codex/skills/doc/SKILL.md)
- gh-address-comments: Help address review/issue comments on the open GitHub PR for the current branch using gh CLI; verify gh auth first and prompt the user to authenticate if not logged in. (file: ~/.codex/skills/gh-address-comments/SKILL.md)
- gh-fix-ci: Use when a user asks to debug or fix failing GitHub PR checks that run in GitHub Actions; use `gh` to inspect checks and logs, summarize failure context, draft a fix plan, and implement only after explicit approval. Treat external providers (for example Buildkite) as out of scope and report only the details URL. (file: ~/.codex/skills/gh-fix-ci/SKILL.md)
- imagegen: Use when the user asks to generate or edit images via the OpenAI Image API; run the bundled CLI (`scripts/image_gen.py`) and require `OPENAI_API_KEY` for live calls. (file: ~/.codex/skills/imagegen/SKILL.md)
- openai-docs: Use when the user asks how to build with OpenAI products or APIs and needs up-to-date official documentation with citations. (file: ~/.codex/skills/openai-docs/SKILL.md)
- pdf: Use when tasks involve reading, creating, or reviewing PDF files where rendering and layout matter. (file: ~/.codex/skills/pdf/SKILL.md)
- playwright: Use when the task requires automating a real browser from the terminal. (file: ~/.codex/skills/playwright/SKILL.md)
- screenshot: Use when the user explicitly asks for a desktop or system screenshot. (file: ~/.codex/skills/screenshot/SKILL.md)
- skill-creator: Guide for creating effective skills. (file: ~/.codex/skills/.system/skill-creator/SKILL.md)
- skill-installer: Install Codex skills into $CODEX_HOME/skills. (file: ~/.codex/skills/.system/skill-installer/SKILL.md)

### How to use skills

- Trigger rules: If the user names a skill (with `$SkillName` or plain text) OR the task clearly matches a skill's description, use that skill for that turn.
- Progressive disclosure: Open the referenced `SKILL.md` and read only enough to follow the workflow.
- Context hygiene: Keep context small; avoid bulk-loading.

