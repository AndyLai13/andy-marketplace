---
name: cleanup-merged-branch
description: Use when finishing work on a feature branch — typical triggers are "PR was merged, clean up", "we're done with this branch", "switch back to main". Performs end-of-branch cleanup safely; aborts and reports if the branch is not actually merged into main.
---

# cleanup-merged-branch

End-of-branch cleanup: exit the worktree, sync `main`, then delete the local branch — **only** if it's already merged into `main`. Refuses to touch unmerged work.

## When to use

- A PR has been merged and the local branch + worktree should be disposed of
- The user says: "clean up this branch", "PR is merged, switch back to main", "we're done here"

**Don't use when:**
- The branch is not yet merged (this skill aborts; finish/merge the PR first)
- Already on `main` (nothing to clean)
- The user wants to keep the branch around for reference

## Steps

Follow in this order. Each step has a hard abort condition — do not skip ahead.

### 1. Record the branch name BEFORE exiting the worktree

```bash
BRANCH=$(git rev-parse --abbrev-ref HEAD)
```

- If `BRANCH` is `main` or `HEAD`: **STOP**, report "already on main / detached HEAD, nothing to clean", do not continue.

### 2. Exit the worktree

Invoke the **`ExitWorktree`** tool (Claude Code built-in). It cleans up the linked worktree and returns the working directory to the parent repo.

- If we are NOT in a linked worktree (`git rev-parse --git-dir` equals `git rev-parse --git-common-dir`), skip this step.
- If `ExitWorktree` fails because of uncommitted changes: **STOP** and surface the error. Do not force.

### 3. Switch to `main`

```bash
git checkout main
```

### 4. Sync `main` with the remote (fast-forward only)

```bash
git fetch origin
git pull --ff-only
```

- If `pull --ff-only` fails (local `main` has diverged from `origin/main`): **STOP**. Report the divergence. Do **not** `reset --hard`. Do **not** proceed to delete the branch.

### 5. Verify the branch is merged into `main`

```bash
git branch --merged main | grep -E "^[* ]+${BRANCH}$" >/dev/null
```

- If exit code ≠ 0 (branch NOT in merged list): **STOP**. Report:
  > Branch `<name>` is not merged into `main` (per `git branch --merged`). Aborting cleanup. If this PR was **squash-merged** on GitHub, `git branch --merged` will not see it — verify with `gh pr view <name>` and delete manually with `git branch -D` if confirmed.
- Do **not** use `git branch -D` to "be safe". `-D` bypasses the merge check; that is the opposite of safe.

### 6. Delete the local branch

```bash
git branch -d "$BRANCH"
```

`-d` (lowercase) refuses to delete unmerged branches as a second safety net; if it fails here, treat it the same as step 5 failing.

### 7. Report

State plainly:
- Branch deleted: `<name>`
- Main now at: `<short SHA>` (`git rev-parse --short main`)

## Quick reference

| Step | Command / tool |
|------|----------------|
| Record branch | `git rev-parse --abbrev-ref HEAD` |
| Exit worktree | `ExitWorktree` tool |
| Switch to main | `git checkout main` |
| Sync main | `git fetch && git pull --ff-only` |
| Check merged | `git branch --merged main` |
| Delete | `git branch -d <branch>` |

## Common mistakes

- **Forgetting to capture `BRANCH` before `ExitWorktree`** — after exiting, the cwd and HEAD change. Capture in step 1, not later.
- **Using `git branch -D` to "be safe"** — `-D` skips the merge check; that's the opposite of safe.
- **Running `git pull` instead of `git pull --ff-only`** — can create stray merge commits on `main` if local has unpushed work.
- **Squash-merged PR mistakenly flagged as unmerged** — `git branch --merged` only sees commits whose hashes are in `main`'s history. Squash creates a new commit, so the original branch tip is not "merged" by this check. Abort path is correct; user must confirm via `gh pr` and use `-D` manually.

## Safety invariants

- Never `git branch -D` (force delete) from within this skill
- Never `git reset --hard` on `main`
- Never proceed past step 4 if `pull --ff-only` fails
- Never proceed past step 5 if branch is not in `--merged` list
