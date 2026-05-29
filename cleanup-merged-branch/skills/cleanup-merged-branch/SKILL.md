---
name: cleanup-merged-branch
description: Use when finishing work on a feature branch — typical triggers are "PR was merged, clean up", "we're done with this branch", "switch back to main". Performs end-of-branch cleanup safely; aborts and reports if the branch is not actually merged into main, leaving the worktree and branch untouched.
---

# cleanup-merged-branch

End-of-branch cleanup: sync `main`, verify the branch is merged, **then** remove the worktree and delete the local branch — in that order. If any check before the destructive steps fails, the worktree and branch are left fully intact. (The main repo's HEAD and cwd do shift to `main` — see step 5's abort note.)

## When to use

- A PR has been merged and the local branch + worktree should be disposed of
- The user says: "clean up this branch", "PR is merged, switch back to main", "we're done here"

**Don't use when:**
- The branch is not yet merged into `main` (this skill aborts; finish/merge the PR first)
- Already on `main` with no worktree (nothing to clean)

## Step order — checks first, destruction last

This ordering is the safety guarantee. **Steps 1–5 do not touch the feature branch or its worktree.** If step 5 aborts, the branch ref, the worktree directory, and the worktree's files are all intact. (Steps 2–4 do change cwd and may fast-forward `main` in the main repo — both are desirable side effects, but not "no-op". The abort is non-destructive to the feature work, not state-equivalent overall.)

### 1. Capture context (run from inside the worktree)

```bash
BRANCH=$(git rev-parse --abbrev-ref HEAD)
WORKTREE_PATH=$(git rev-parse --show-toplevel)
MAIN_REPO=$(git worktree list --porcelain | awk '/^worktree /{print $2; exit}')
```

- If `BRANCH` is `main` or `HEAD` (detached): **STOP**, report "nothing to clean up".
- If `WORKTREE_PATH` equals `MAIN_REPO`: the feature work is in the primary worktree, not a linked one — note this; the worktree-remove step (#6) becomes a no-op.

### 2. Move to the main repo's working directory

```bash
cd "$MAIN_REPO"
```

Required: git refuses `checkout main` from a linked worktree if another worktree has `main` checked out, and once a worktree is removed its directory is gone — neither situation allows steps 3–5 to run from the original cwd.

### 3. Ensure HEAD in the main repo is on `main`

```bash
git checkout main
```

- If this fails (uncommitted changes, conflicts in the main repo): **STOP**, surface the error. No cleanup yet.

### 4. Sync `main` with the remote (fast-forward only)

```bash
git fetch origin
git pull --ff-only origin main
```

Explicit `origin main` so a misconfigured upstream can't pull from the wrong place.

- If `pull --ff-only` fails (local `main` diverged from `origin/main`): **STOP**. Do **not** `reset --hard`. No cleanup yet.

### 5. Verify the branch is merged into `main`

```bash
git branch --merged main --format='%(refname:short)' | grep -Fx -- "$BRANCH"
```

`--format='%(refname:short)'` gives one clean branch name per line. `grep -Fx` matches the whole line as a fixed string — safe for any branch name (`feat+x`, `feat[1]`, etc.).

- If exit code ≠ 0: **STOP**. Report:
  > Branch `<name>` is not merged into `main` (per `git branch --merged`). Aborting cleanup — **your branch and worktree are intact**; the only changes were that the main repo is now on `main` (possibly fast-forwarded). If this PR was **squash- or rebase-merged**, the original commits are not in main's history — verify with `gh pr view <name>` (if `gh` is available) and delete manually with `git branch -D` if confirmed.
- Do **not** use `git branch -D` from this skill. `-D` bypasses the merge check.

**Step 5 is the last point at which abort is a true no-op. Everything past here is destructive.**

### 6. Remove the worktree

Default to the universal bash form:

```bash
git worktree remove "$WORKTREE_PATH"
```

**Use the `ExitWorktree` tool instead only if** the *current Claude Code session itself* invoked `EnterWorktree` to enter this worktree — `ExitWorktree` then also releases the session's association. If you don't know for sure (worktree was created by `/gwt`, `git worktree add`, or a previous session), use the bash form — it always works.

- Skip this step entirely if step 1 found `WORKTREE_PATH == MAIN_REPO`.
- If removal fails (uncommitted changes in the worktree): **STOP**. Do **not** `--force`. The branch deletion in step 7 also doesn't run.

### 7. Delete the local branch

```bash
git branch -d "$BRANCH"
```

`-d` (lowercase) refuses unmerged branches as a second safety net — if it errors here, the merge state contradicts step 5; investigate, don't override.

### 8. Report

- Branch deleted: `<name>`
- Main now at: `<short SHA>` from `git rev-parse --short HEAD`

## Quick reference

| Step | Tool / command | Destructive? |
|------|----------------|--------------|
| 1 | `git rev-parse` / `git worktree list --porcelain` | No |
| 2 | `cd "$MAIN_REPO"` | No |
| 3 | `git checkout main` | No |
| 4 | `git fetch` + `git pull --ff-only origin main` | No |
| 5 | `git branch --merged main --format='%(refname:short)' \| grep -Fx` | No |
| 6 | `ExitWorktree` tool **or** `git worktree remove "$WORKTREE_PATH"` | **Yes** |
| 7 | `git branch -d "$BRANCH"` | **Yes** |

## Common mistakes

- **Removing the worktree before the merge check.** If step 5 aborts later, the user has lost their workspace but the branch is still around. The order in this skill is non-negotiable: checks first, destruction last.
- **Using `git branch -D` to "be safe".** `-D` bypasses the merge check; that's the opposite of safe.
- **`git pull` without `--ff-only`** — can create stray merge commits on `main`.
- **`grep -E "${BRANCH}$"` against `git branch --merged`** — fragile for branch names with regex metacharacters. Use `--format='%(refname:short)'` + `grep -Fx`.
- **Squash-merged PR flagged as unmerged.** Squash creates a new commit, so the original branch tip is not in `main`'s history. The skill's abort path is correct; user verifies via `gh pr` and uses `-D` manually.

## Safety invariants

- Non-destructive operations (1–5) always come before destructive operations (6–7)
- Never `git branch -D` from this skill
- Never `git reset --hard` on `main`
- Never `git worktree remove --force`
- Abort at step 5 leaves the feature branch ref and worktree directory untouched (cwd and `main` HEAD may have shifted — that's not state-equivalent, but it is non-destructive to the feature work)
