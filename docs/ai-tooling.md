# AI Tooling

Which AI/context tools were actually used on this project, and which were considered but not installed.

---

## Used

| Tool / practice | How it was used |
|-----------------|-----------------|
| **Cursor** (Agent / chat) | Spec drafting, implementation, review, and documentation across sessions |
| **SpecStory history** (`.specstory/history/`) | Durable capture of major development sessions (requirements, architecture, state machine, implementation, portal) |
| **Repository specs & Cursor rules** | Primary grounding: `spec/*`, `.cursor/rules/*` — not chat memory alone |
| **`docs/prompt-history.md` / `docs/ai-review.md`** | Human-readable session map and AI-mistake evidence |

---

## Considered but not installed

| Tool | Why not installed for this final pass |
|------|----------------------------------------|
| **Graphify** | Assignment mentions it for token/context optimisation; not already configured in this repo. Installing now would add setup risk without changing product behaviour. |
| **Caveman** | Same: not present in the project tooling; skipped to avoid destabilising the demo. |
| **Codebase-memory MCP** | Not configured in this workspace’s MCP catalog for this project. Specs + SpecStory + Cursor rules already provide the needed long-term context. |

**Principle:** Prefer existing specs and history over introducing large new AI dependencies late in the assignment.

---

## Not claimed

This document does **not** claim that Graphify, Caveman, or Codebase-memory MCP were used. They were considered and consciously deferred.
