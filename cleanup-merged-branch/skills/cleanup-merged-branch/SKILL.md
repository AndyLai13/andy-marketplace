---
name: cleanup-merged-branch
description: Use when finishing work on a feature branch — typical triggers are "PR was merged, clean up", "we're done with this branch", "switch back to main". Performs end-of-branch cleanup safely; recognizes both standard merges (via git) and squash/rebase merges (via `gh pr` when available); aborts cleanly if the branch is not actually merged, leaving the worktree and branch untouched.
---

# cleanup-merged-branch

End-of-branch cleanup: sync `main`, verify the branch is merged (standard merge **or** squash/rebase merge via `gh`), then remove the worktree and delete the local branch. Checks come before destruction — on abort, the feature branch ref and its worktree directory are untouched.

## When to use

- A PR has been merged and the local branch + worktree should go away
- User says "clean up this branch", "PR merged, back to main", "we're done here"

**Don't use when:** branch isn't merged yet; already on `main` with no worktree.

## Steps — non-destructive (1–5) before destructive (6–7)

If step 5 aborts, the feature branch ref and worktree are intact. (cwd shifts to `$MAIN_REPO`; `main`'s HEAD may fast-forward — both intentional, not "no-op".)

**Run all steps in the same shell** so `BRANCH`, `WORKTREE_PATH`, `MAIN_REPO`, and `MERGE_KIND` persist. If your harness invokes each step as a separate bash call, persist these to a temp file (e.g. `/tmp/cleanup-vars-$$`) and re-source it each step.

### 1. Capture context (from inside the worktree)

```bash
BRANCH=$(git rev-parse --abbrev-ref HEAD)
WORKTREE_PATH=$(git rev-parse --show-toplevel)
MAIN_REPO=$(git worktree list --porcelain | awk '/^worktree /{print $2; exit}')
```

STOP if `BRANCH` is `main` or `HEAD` (detached).
If `WORKTREE_PATH == MAIN_REPO`: not in a linked worktree; step 6 becomes a no-op.

### 2. Move to the main repo

```bash
cd "$MAIN_REPO"
```

Required: git refuses `checkout main` from a linked worktree where `main` is checked out elsewhere.

### 3. Switch to `main`

```bash
git checkout main
```

STOP if it fails (uncommitted changes in the main repo).

### 4. Sync `main`

```bash
git fetch origin
git pull --ff-only origin main
```

STOP if `pull --ff-only` fails (divergence). Never `reset --hard`.

### 5. Verify the branch is merged — standard, then squash/rebase via `gh`

**5a. Standard merge check:**
```bash
if git branch --merged main --format='%(refname:short)' | grep -Fx -- "$BRANCH"; then
  MERGE_KIND=standard
fi
```

**5b. If `MERGE_KIND` still unset, try `gh` (handles squash and rebase merges):**
```bash
if [ -z "${MERGE_KIND:-}" ] && command -v gh >/dev/null 2>&1 && \
   gh pr list --state merged --head "$BRANCH" --json number --limit 1 2>/dev/null \
     | grep -q '"number"'; then
  MERGE_KIND=squash
fi
```

Falls through silently if `gh` is missing, not authenticated, the remote isn't GitHub, or no merged PR exists for `$BRANCH`.

**5c. If `MERGE_KIND` still unset: STOP.** Report:
> Branch `<name>` is not merged into `main` (and `gh` could not confirm a merged PR). Aborting — **branch and worktree are intact**; only `main` may have been fast-forwarded. Verify manually if your PR provider isn't GitHub or `gh` isn't installed.

**Step 5 is the last point at which abort preserves the branch and worktree.**

### 6. Remove the worktree

Default form (works universally):
```bash
git worktree remove "$WORKTREE_PATH"
```

Use the `ExitWorktree` tool **only if** the current Claude Code session itself invoked `EnterWorktree` to enter this worktree — it also releases the CC session association. When in doubt, use bash.

Skip if step 1 found `WORKTREE_PATH == MAIN_REPO`. If removal fails (uncommitted changes), STOP — do not `--force`.

### 7. Delete the local branch

```bash
case "$MERGE_KIND" in
  standard) git branch -d "$BRANCH" ;;   # -d refuses unmerged work (second safety net)
  squash)   git branch -D "$BRANCH" ;;   # -d would refuse; gh already confirmed merged PR
esac
```

If `-d` errors in the `standard` case, step 5a contradicts itself — investigate, do not override.

### 8. Report

- Branch deleted: `<name>` (kind: `$MERGE_KIND`)
- Main now at: `<short SHA>` from `git rev-parse --short HEAD`

## Safety invariants

- Non-destructive (1–5) always before destructive (6–7)
- `git branch -D` only when `MERGE_KIND=squash` (i.e. `gh` confirmed a merged PR)
- Never `git reset --hard` on `main`
- Never `git worktree remove --force`
- Squash/rebase recognition needs `gh` + GitHub PR; other providers require manual verification and manual `-D`
- Abort at step 5 leaves the feature branch ref and worktree directory untouched (cwd and `main` HEAD may have shifted)

## Common pitfalls

- **`-D` "to be safe"** — bypasses checks; only allowed via the explicit squash path
- **`git pull` without `--ff-only`** — can create stray merges on `main`
- **Regex grep on branch name** — always `--format='%(refname:short)' | grep -Fx`, never `grep -E "$BRANCH$"`
- **Squash-merged PR appears unmerged to git** — that's why step 5b exists; needs `gh` + GitHub
- **Removing the worktree before the merge check** — leaves a dirty state on abort; ordering is non-negotiable
