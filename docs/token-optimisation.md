Review the existing Support Ticket Management System repository.

Create docs/token-optimisation.md documenting how AI context and token usage were optimized during development.

Document only techniques actually used or configured in this repository.

Cover:
1. Reusable .cursor/rules used to avoid repeating project instructions.
2. Spec-driven development and how spec/ files were used as focused context.
3. Separation of requirements, architecture, data model, API contract, state machine, UI flow and test strategy.
4. Prompt history through .specstory/history.
5. Any MCP/plugin-based codebase navigation or context optimization that is actually configured and used.
6. How large unnecessary context was avoided.
7. How AI was asked to inspect only relevant files before making changes.
8. How AI-generated suggestions were reviewed instead of blindly accepted.

IMPORTANT:
- Do not claim Graphify, Caveman or Codebase-memory MCP was used unless it is actually installed/configured and there is evidence in the repository.
- If none of these tools were actually used, explicitly document that they were evaluated as possible token-optimization tools but were not used.
- Do not invent usage evidence.
- Do not modify application code.
- Do not modify existing specifications.
