---
name: run-doc-lifecycle
description: Use when an implementation plan has finished shipping and the draft + plan files need to be absorbed into canonical specs and deleted — the draft → plan → ship → spec lifecycle's closing step. Triggers — user says "本任務完成" / "task complete" / "收尾 lifecycle" / "歸檔 plan" / "plan 跑完了把文件收掉"; drafts/plans in docs/superpowers/ accumulating un-absorbed after features ship. This is the executor counterpart to dev-bootstrap (which only scaffolds the lifecycle). Gated on user QA confirmation — never auto-run.
---

# run-doc-lifecycle

Closes the **draft → plan → ship → spec** doc lifecycle for one shipped plan: map every design fact in the plan + its draft to the canonical specs, absorb what's missing, verify absorption is complete, then delete the plan. The destructive step (deleting working files) comes **last** and only after verification — git history is the only recovery path, so a fact lost from canonical before deletion is a real loss.

The originating draft was usually **already deleted when the plan was written** (see *Draft deletion timing* below) — so this skill normally deletes only the plan.

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

**Deleting the plan (and, on its last sibling, a shared one-to-many draft) is the only irreversible action. It happens last, and only after every design fact is verified present in canonical.** Absorbing-then-deleting in one unverified motion is the failure this skill exists to prevent.

## Draft deletion timing (deleted at planning time, not here)

A draft is deleted **as soon as it is written into a plan or spec** — back at brainstorming / writing-plans time — not at ship time. Once the content lives in a plan/spec, the draft is redundant; git history retains it. So by the time this skill runs, a single-plan draft is **normally already gone**, and this skill deletes the **plan**.

**Exception — one-to-many drafts.** A draft that spawns *multiple* plans is kept until **all** its derived plans have shipped and been deleted. When you run this skill for such a plan, delete the shared draft **only if this is the last sibling** (no other un-shipped plan still traces back to it); otherwise leave it for the final sibling's lifecycle.

This is the project owner's stated preference and overrides the older "delete plan + draft together" model.

## Execution flow

Follow in order. Each gate must pass before advancing.

### Gate 0 — QA confirmed

Confirm the user has said QA / review passed for this plan. If unsure, ask. Do not run on "the last task's tests are green" alone — the plan stops at the user's QA checkpoint.

### 1. Read the project's lifecycle definition

Find and read the project's lifecycle doc (usually `docs/product/README.md` or `docs/product/spec/README.md`). Extract:
- **Canonical location** — where shipped design facts live (`docs/product/*.md` or `docs/product/spec/*.md`)
- **Changelog format** — the exact row shape used in its 變更歷史 / changelog table
- **Deletion rule** — confirm the project deletes the plan on absorption (git history retained); single-plan drafts were already deleted at planning time, so don't expect them present

If no lifecycle doc exists, the project hasn't adopted this lifecycle — stop and suggest `dev-bootstrap`.

### 2. Map plan + draft → affected canonical docs

Read the plan in full — and its originating draft if it still exists. Single-plan drafts are normally already deleted (at planning time); recover the original with `git show <deleted-draft-path>` or read the spec that absorbed it. A draft that *is* still present is a one-to-many draft — read it too. Enumerate **every** design fact, decision, schema change, API/endpoint, RPC, RLS policy, and UI change. For each, name the canonical file it belongs in. Produce a mapping table — do **not** guess "it's just schema.md." One plan usually touches several canonical files (schema + api + auth + ui-ia, etc.).

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

### 7. Delete the plan (and a one-to-many draft only on its last sibling)

The single-plan draft was already deleted at planning time, so usually only the plan remains:

```bash
git rm docs/superpowers/plans/<plan-file>.md
```

If an originating draft is **still present**, it is a one-to-many draft (kept because it spawned multiple plans). Delete it **only when this is the last derived plan** — verify no other un-shipped plan still traces back to it. Otherwise leave it for the final sibling's lifecycle:

```bash
# ONLY when this is the final plan derived from the shared draft:
git rm docs/superpowers/drafts/<shared-draft>.md
```

### 8. Commit (no push)

```bash
git add <changed canonical files>
git commit -m "chore(lifecycle): <topic> absorbed — plan deleted, spec log updated"
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
| Expecting the draft to still be there | Single-plan drafts are deleted at planning time. Recover via git history if you need them; don't treat their absence as a missing step. |
| Deleting a shared draft too early | A one-to-many draft stays until its LAST derived plan ships. Deleting it on an earlier sibling orphans the remaining plans' source. |
| Pushing the lifecycle commit | Don't. Local commit only; the user pushes. |

## Red flags — STOP

- About to `git rm` a plan/draft without having re-verified every fact is in canonical
- "The execution probably absorbed it already" — verify, don't assume
- "QA is basically done" — confirm explicitly with the user first
- Mapping a multi-file plan to a single canonical doc

## Why this design

The project's README owns the *format*; this skill owns the *discipline*. That keeps the skill reusable across any project that adopted `dev-bootstrap`'s lifecycle, while guaranteeing the one irreversible step (deletion) never runs ahead of verified absorption.
