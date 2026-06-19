---
name: dev-bootstrap
description: Use when a new project is ready to adopt the brainstorm-draft → implementation-plan → canonical-spec doc lifecycle — typically after a brainstorm has settled on a project name, one-liner, and rough scope, and `docs/product/spec/` does not yet exist. Two entry points — (a) explicit: user runs `/toolbox:dev-bootstrap`; (b) proactive: when those readiness signals appear in a greenfield repo, OFFER to run it (ask first — this skill creates files and commits, never auto-execute). Sets up docs/product/spec/, docs/superpowers/{drafts,plans,runbooks}/, root README.md, CLAUDE.md, glossary stub, and optionally docs/product/testing/ (Pyramid-leaning Trophy structure that references the test-strategy skill). Greenfield only — aborts if docs/product/spec/ already exists.
---

# dev-bootstrap

One-shot scaffold for a project that adopts the **draft → plan → ship → spec** doc lifecycle. Designed for solo or small-team dev where `docs/product/spec/` is the single source of truth and `docs/superpowers/{drafts,plans}/` are ephemeral working files that get distilled into specs at task-complete time.

## How this skill activates

Two entry points:

1. **Explicit** — user runs `/toolbox:dev-bootstrap`. Deterministic, safe path. Jump straight to the execution flow.
2. **Proactive (readiness detection)** — during a conversation, propose running this skill when **all three** signals hold:
   - The discussion is brainstorming a **new project** and has settled on at least a project name, a one-liner, and rough scope.
   - cwd is a git repo (or the user is fine with `git init`).
   - `docs/product/spec/` does **not** exist (greenfield).

   When the signals hold, **offer** — e.g. "Looks like this project is ready to scaffold its doc lifecycle. Want me to run dev-bootstrap?" — and wait for a yes.

**Never auto-execute on detection.** This skill creates files and writes a commit; mid-brainstorm surprise scaffolding is a bad outcome and early readiness is easy to misjudge. Detection ⇒ propose; only an explicit yes (or the slash command) ⇒ run.

## When to use

✅ User is initializing a new project (or retrofitting one that has no `docs/` yet)
✅ Project is already a git repo (or user is OK with `git init` first)
✅ User wants a consistent doc lifecycle that survives across AI tools (Claude / Codex / Cursor / Gemini) — rules live in repo, not in agent memory

## When NOT to use

❌ `docs/product/spec/` already exists — abort, do not overwrite (greenfield only)
❌ User only wants behavioural reminders for an existing project — point them at the existing `README.md` / `CLAUDE.md` instead
❌ User wants research-only docs (competitors / interviews) without lifecycle structure — just `mkdir` those folders manually

## Execution flow

Follow these steps **in order**. Each step has a clear gate; do not advance until the gate passes.

### 1. Greenfield gate

The lifecycle structure is the source of truth, so the only hard gate is `docs/product/spec/`:

```bash
test -d docs/product/spec && echo "ABORT: docs/product/spec/ already exists"
```

If exists → stop and tell the user. Do not offer merge mode.

`README.md` and `CLAUDE.md` get **soft collision handling** in steps 4 and 5 (append / skip). They do not gate the run.

If the cwd is not a git repo, ask the user whether to `git init` before continuing. Do not init silently.

### 2. Gather inputs (use AskUserQuestion)

Ask all five questions in one call:

1. **Project name** — used in README H1 and CLAUDE.md (free text; suggest the cwd folder name)
2. **One-line description** — used under H1 (free text)
3. **Git workflow** — `solo` (direct merge to main, no PR) or `team` (PR + review)
4. **Research subdirs** (multi-select) — `business-model`, `competitors`, `interviews`, `market-research`. Create empty dir + `.gitkeep` for each chosen.
5. **Testing docs structure** — `yes` (scaffold `docs/product/testing/` with 7 templates + SVG referencing the `test-strategy` skill) or `no` (skip; user can add later). Default recommendation: **yes** for any project that will have automated tests.

The workflow answer picks which `WORKFLOW_BLOCK` to inject. The research answer decides which empty dirs to create. The testing answer decides whether step 7 runs.

### 3. Create directory skeleton

Always create:

```
docs/product/spec/
docs/superpowers/drafts/
docs/superpowers/plans/
docs/superpowers/runbooks/
docs/reference/
```

Add `.gitkeep` to every directory that will not receive a real file in this run. After step 6:

- `docs/product/spec/` gets `README.md` + `overview.md` → no .gitkeep needed
- `docs/reference/` gets `glossary.md` → no .gitkeep needed
- `docs/superpowers/drafts/`, `docs/superpowers/plans/`, `docs/superpowers/runbooks/` start empty → **add `.gitkeep`** to each (unless the dir already existed with content, in which case skip)

For each chosen research subdir, create `docs/{subdir}/.gitkeep`.

### 4. Root README.md (collision-aware)

Decide which path:

| Condition | Action |
|---|---|
| `./README.md` does **not** exist | Render `templates/README.md` → `./README.md` (greenfield path) |
| `./README.md` exists AND already contains `## 開發協作規約` | **Skip** and warn user: "README already has 開發協作規約 section — leaving it alone, please reconcile manually if needed." |
| `./README.md` exists AND does **not** contain `## 開發協作規約` | Render `templates/README-append.md` and **append** to the existing README (preserves H1, one-liner, and any existing sections) |

For both render paths, apply the placeholder substitution and workflow-block selection below.

### 5. CLAUDE.md (collision-aware)

| Condition | Action |
|---|---|
| `./CLAUDE.md` does **not** exist | Render `templates/CLAUDE.md` → `./CLAUDE.md` |
| `./CLAUDE.md` exists | **Skip** and warn user: "CLAUDE.md already exists — leaving it alone. The lifecycle protocol is also in README.md and docs/product/spec/README.md; you may want to reconcile manually." |

### 6. Other files (always render — guarded by greenfield gate in step 1)

| Template | Output path |
|---|---|
| `templates/spec-README.md` | `./docs/product/spec/README.md` |
| `templates/spec-overview-stub.md` | `./docs/product/spec/overview.md` |
| `templates/glossary.md` | `./docs/reference/glossary.md` |

### Placeholder substitution rules (for all render paths)

Simple text replace, not a templating engine:

| Placeholder | Source |
|---|---|
| `{{PROJECT_NAME}}` | Q1 answer |
| `{{ONE_LINER}}` | Q2 answer |
| `{{TODAY}}` | Today's date in `YYYY-MM-DD` (read from environment context) |

**Workflow-block selection** (`templates/README.md` and `templates/README-append.md` only):

Both templates have two alternative blocks delimited by HTML-comment markers:

```
<!-- KEEP-IF-SOLO-START -->
...solo block content...
<!-- KEEP-IF-SOLO-END -->

<!-- KEEP-IF-TEAM-START -->
...team block content...
<!-- KEEP-IF-TEAM-END -->
```

Based on Q3:

- **Solo**: delete the team block entirely (markers + content). Delete the solo `<!-- KEEP-IF-SOLO-START -->` and `<!-- KEEP-IF-SOLO-END -->` marker lines but keep the content between them.
- **Team**: mirror — delete solo block entirely, strip team markers but keep team content.

The rendered output should contain exactly one workflow block and no `KEEP-IF` marker comments. For `README-append.md`, also remove the top HTML comment block (the one explaining what the file is).

### 7. Testing docs scaffold (only if Q5 = yes)

If `docs/product/testing/` already exists → **skip with warning**: "testing docs already exist — leaving them alone."

Otherwise, render the 8 testing templates with placeholder substitution (`{{PROJECT_NAME}}`, `{{TODAY}}`):

| Template | Output path |
|---|---|
| `templates/testing/README.md` | `./docs/product/testing/README.md` |
| `templates/testing/shape.md` | `./docs/product/testing/shape.md` |
| `templates/testing/patterns.md` | `./docs/product/testing/patterns.md` |
| `templates/testing/coverage.md` | `./docs/product/testing/coverage.md` |
| `templates/testing/ci.md` | `./docs/product/testing/ci.md` |
| `templates/testing/multi-tenant-safety.md` | `./docs/product/testing/multi-tenant-safety.md` |
| `templates/testing/status.md` | `./docs/product/testing/status.md` |
| `templates/testing/test-pyramid-shapes.svg` | `./docs/product/testing/test-pyramid-shapes.svg` |

All 8 files include cross-links to each other + an outbound link to the `test-strategy` skill as the conceptual source. The doctrine itself is NOT duplicated into the project — `shape.md` is the project-specific instantiation.

If the project is not multi-tenant, the user can delete `multi-tenant-safety.md` after the bootstrap.

### 8. Verify

Before committing, run:

```bash
test -f README.md && test -f docs/product/spec/README.md
ls docs/superpowers/{drafts,plans,runbooks}/ docs/reference/
# if Q5 = yes:
test -f docs/product/testing/README.md && test -f docs/product/testing/shape.md
```

`CLAUDE.md` is allowed to be absent if step 5 hit the "exists, skipped" branch.

If a required check fails, surface the error and stop. Do not commit a half-bootstrapped tree.

### 9. Commit

```bash
git add README.md docs/
git add CLAUDE.md  # only if step 5 wrote it (don't `git add` non-existent files)
git commit -m "chore: bootstrap doc lifecycle structure"
```

Do not push. The user pushes when they're ready.

### 10. Hand-off message

Print a short next-steps message:

- First brainstorm session → `docs/superpowers/drafts/{TODAY}-{topic}.md`
- When ready to implement → `docs/superpowers/plans/{TODAY}-{topic}-implementation.md`
- After ship → user says "本任務完成" / "task complete" → execute lifecycle protocol from `README.md`
- (if Q5=yes) First testing pass → fill in `docs/product/testing/shape.md` with project-specific RPC / RLS / vendor names; run first test, update `docs/product/testing/status.md` via the refresh script in its footer

## Common mistakes

| Mistake | Fix |
|---|---|
| Running the skill without checking greenfield first | The greenfield gate is non-negotiable. If `docs/product/spec/` exists, abort — even if the user insists. |
| Picking both workflow blocks "to be safe" | Pick ONE based on Q3. README must be unambiguous. |
| Pre-creating cross-cutting spec stubs (api/schema/auth/...) | Don't. Only `overview.md` stub. Other specs get created when the first feature lands. |
| Pre-creating runbook templates | Don't. Runbooks are created when there's a real ops need. |
| Pushing the commit | Don't. The user decides when to push. |
| Skipping CLAUDE.md because "README has it all" | CLAUDE.md is auto-injected into Claude system prompt; it's the condensed contract for AI sessions. Keep both. |
| Overwriting an existing README without checking | Step 4 has explicit collision handling. Check `## 開發協作規約` substring before deciding render vs append vs skip. |
| Forcing a CLAUDE.md write when one exists | Step 5 explicitly skips. The user's existing CLAUDE.md may encode project-specific instructions you'd destroy. Warn instead. |
| Copying the `test-strategy` content into `shape.md` | Don't. `shape.md` just *references* the skill via URL. Duplicating the doctrine creates a drift surface. |
| Running step 7 when `docs/product/testing/` already exists | Skip with warning — same rule as README/CLAUDE collision. Never overwrite an existing testing-doc tree. |

## Why this design

Rules live in the **project's own files** (README.md, CLAUDE.md, docs/product/spec/README.md), not in this skill's body. The skill is just a scaffolder. Once it runs, the project is self-describing — any AI tool (Claude / Codex / Cursor / Gemini) reading the repo will pick up the lifecycle rules without needing this skill installed.

That's why this skill's templates carry the substantive content, and `SKILL.md` only carries execution flow.
