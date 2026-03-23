# Contributing (Cursor Guidance)

## Cursor Governance

- This repository is governed independently from other repos.
- Backend Cursor owner should review any changes to:
  - `CLAUDE.md`
  - `.cursor/**`

## Team Defaults

- Repo-wide instructions: `CLAUDE.md`
- Modular rules: `.cursor/rules/`
- Team commands: `.cursor/commands/`
- Local-only overrides (not committed):
  - `CLAUDE.local.md`
  - `.cursor/settings.local.json`

## PR Checklist Additions

- Cursor guidance files changed intentionally and reviewed.
- Any new rule/command is scoped to backend needs.
- Validation status is explicitly reported in PR notes.

## Rollout

1. Pilot rule/command changes with 1-2 maintainers.
2. Collect friction and adjust within this repo only.
3. Revisit monthly or when workflow pain is reported.
