---
name: remove-task
description: Use when the user invokes `/remove-task <ref>` to delete a task from the session task list. `<ref>` identifies the task by its `#N` label, subject text, or task ID (one or more, space/comma-separated). A task already **completed** is deleted immediately; a task still **pending or in_progress** is deleted only after confirming with the user. Companion to `add-tasks`. Not for marking work done (use TaskUpdate status=completed), not for adding tasks (use `add-tasks`).
---

# remove-task — delete tasks, gated by completion state

Remove tasks from the session task list. **Completed** tasks are deleted immediately; **not-yet-completed** (pending / in_progress) tasks require the user's confirmation first, so live work is never dropped by accident.

## When to use

✅ User invokes `/remove-task <ref>` to drop one or more tasks
✅ `<ref>` names tasks by `#N`, subject text, or task ID

## When NOT to use

❌ Marking a task finished (keeping it in the list) → `TaskUpdate` status=completed
❌ Adding tasks → use `add-tasks`
❌ Editing a task's subject/description → `TaskUpdate`

## Procedure

Create a todo per step and work them in order.

### 1. Load the current list

Call `TaskList`. Record each task's **id**, **subject** (including any `#N` label), and **status**.

### 2. Resolve `<ref>` to task(s)

Split `<ref>` on spaces/commas into one or more references. For each, match against the list in this order:
1. **`#N` label** — matches the task whose subject carries that `#N`
2. **task ID** — exact internal id
3. **subject text** — substring / semantic match on the subject

- **No match** → report it, do not delete anything for that ref.
- **Ambiguous** (multiple matches) → list the candidates and ask the user which one; do not guess.

### 3. Partition matched tasks by status

- **completed** → the *delete-now* set
- **pending / in_progress** → the *confirm-first* set

### 4. Delete completed tasks immediately

For each task in the delete-now set, call `TaskUpdate` with `{ taskId, status: "deleted" }`. No confirmation needed — the work is already done.

### 5. Confirm before deleting unfinished tasks

If the confirm-first set is non-empty, **stop and ask the user**, listing each task as `#N <subject> (<status>)`. Wait for an explicit yes.
- On confirmation → `TaskUpdate { taskId, status: "deleted" }` for the confirmed tasks.
- If the user declines (or picks a subset) → leave the rest untouched.

Do NOT delete any pending/in_progress task without this confirmation, even when it appears in the same `/remove-task` call as completed tasks.

### 6. Report

Summarize concisely:
- **已刪除（完成）**: the tasks removed in step 4
- **已刪除（確認後）**: the tasks removed in step 5
- **保留 / 未確認**: unfinished tasks the user chose to keep
- **找不到 / 不明確**: refs that matched nothing or were ambiguous

## Notes

- Deletion uses `TaskUpdate` status=`deleted` (permanent — the task-list has no undo).
- `#N` labels are not renumbered after a delete; gaps are fine and existing tasks keep their numbers. `add-tasks` continues from the current max regardless of gaps.
- Follow the session's response language for prose (this user: 繁體中文).
