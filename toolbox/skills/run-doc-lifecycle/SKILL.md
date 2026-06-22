---
name: run-doc-lifecycle
description: Use when an implementation plan has finished shipping and the draft + plan files need to be absorbed into canonical specs and deleted — the draft → plan → ship → spec lifecycle's closing step. Triggers — user says "本任務完成" / "task complete" / "收尾 lifecycle" / "歸檔 plan" / "plan 跑完了把文件收掉"; drafts/plans in docs/superpowers/ accumulating un-absorbed after features ship. This is the executor counterpart to dev-bootstrap (which only scaffolds the lifecycle). Gated on user QA confirmation — never auto-run.
---

# run-doc-lifecycle

Closes the **draft → plan → ship → spec** doc lifecycle for one shipped plan: map every design fact in the plan + its draft to the canonical specs, absorb what's missing, verify absorption is complete, then delete the plan + draft. The destructive step (deleting working files) comes **last** and only after verification — git history is the only recovery path, so a fact lost from canonical before deletion is a real loss.

This skill enforces *discipline* (mapping, verify-before-delete, confirm gate); it reads the project's own README for the *format* (canonical location, changelog convention). Do not hardcode paths — projects differ (some use `docs/product/spec/`, some flatten to `docs/product/`).

## When to use

✅ A plan in `docs/superpowers/plans/` finished executing and its code shipped
✅ User confirmed QA / review passed and wants the docs collapsed into canonical
✅ Un-absorbed drafts/plans are piling up and need reconciling

## When NOT to use

❌ Plan's last task ran but user QA is still pending — STOP at the QA checkpoint, do not finalize (this is the most common premature trigger)
❌ Setting up a *new* project's lifecycle — that's `dev-bootstrap`
❌ Branch cleanup (worktree / local branch) — that's `cleanup-merged-branch`

## The irreversible-step rule

**Deleting the plan + draft is the only irreversible action. It happens last, and only after every design fact is verified present in canonical.** Absorbing-then-deleting in one unverified motion is the failure this skill exists to prevent.

## Execution flow

Follow in order. Each gate must pass before advancing.

### Gate 0 — QA confirmed

Confirm the user has said QA / review passed for this plan. If unsure, ask. Do not run on "the last task's tests are green" alone — the plan stops at the user's QA checkpoint.

### 1. Read the project's lifecycle definition

Find and read the project's lifecycle doc (usually `docs/product/README.md` or `docs/product/spec/README.md`). Extract:
- **Canonical location** — where shipped design facts live (`docs/product/*.md` or `docs/product/spec/*.md`)
- **Changelog format** — the exact row shape used in its 變更歷史 / changelog table
- **Deletion rule** — confirm the project deletes plan + draft on absorption (git history retained)

If no lifecycle doc exists, the project hasn't adopted this lifecycle — stop and suggest `dev-bootstrap`.

### 2. Map plan + draft → affected canonical docs

Read the plan AND its originating draft in full. Enumerate **every** design fact, decision, schema change, API/endpoint, RPC, RLS policy, and UI change. For each, name the canonical file it belongs in. Produce a mapping table — do **not** guess "it's just schema.md." One plan usually touches several canonical files (schema + api + auth + ui-ia, etc.).

### 3. Classify absorption status of each fact

For each mapped fact, read/grep the target canonical file and classify:
- **present** — already absorbed (often happens during execution)
- **missing** — not in canonical yet
- **partial** — present but stale/incomplete

### 4. Propose (semi-automatic confirm gate)

Present to the user before writing anything:
- The absorption edits for every **missing** / **partial** fact (which file, what content)
- The changelog row text, in the project's exact format

Get explicit confirmation. Absorbing content is judgment-heavy — the user approves the mapping and wording before it lands. Do not skip to writing.

### 5. Write absorption + changelog

Apply the confirmed edits to the canonical files. Add the changelog row in the format from step 1.

### 6. Verify-before-delete gate

Re-check **every** fact from step 2 is now present in canonical (re-grep the target files). Only when all are verified **present** do you proceed. If any fact is still missing, fix it and re-verify. **Never delete on assumption.**

### 7. Delete plan + draft

```bash
git rm docs/superpowers/plans/<plan-file>.md
git rm docs/superpowers/drafts/<draft-file>.md   # if a matching draft exists
```

Match the draft by topic/date; if no originating draft exists, delete only the plan.

### 8. Commit (no push)

```bash
git add <changed canonical files>
git commit -m "chore(lifecycle): <topic> absorbed — plan + draft deleted, spec log updated"
```

Do not push. Per solo-dev convention the lifecycle commit stays local until the user pushes — and it must not race ahead of their QA sign-off.

## Common mistakes

| Mistake | Fix |
|---|---|
| Deleting plan/draft before verifying absorption | Step 6 gate is non-negotiable. Re-grep every fact. Deletion is last. |
| Guessing one canonical file from the plan title | Step 2: read plan + draft fully, map every fact. Plans touch multiple specs. |
| Writing absorption without user confirm | Step 4 is a hard gate. Absorption wording is the user's call. |
| Running before QA passed | Gate 0. "Tests green" ≠ "QA passed". Wait for the user. |
| Hardcoding `docs/product/spec/` | Step 1: read the project's README. Some projects flattened to `docs/product/`. |
| Forgetting the draft | A plan usually has an originating draft. Both get deleted. |
| Pushing the lifecycle commit | Don't. Local commit only; the user pushes. |

## Red flags — STOP

- About to `git rm` a plan/draft without having re-verified every fact is in canonical
- "The execution probably absorbed it already" — verify, don't assume
- "QA is basically done" — confirm explicitly with the user first
- Mapping a multi-file plan to a single canonical doc

## Why this design

The project's README owns the *format*; this skill owns the *discipline*. That keeps the skill reusable across any project that adopted `dev-bootstrap`'s lifecycle, while guaranteeing the one irreversible step (deletion) never runs ahead of verified absorption.
