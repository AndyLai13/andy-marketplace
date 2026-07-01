---
name: add-tasks
description: Use when the user invokes `/add-tasks <items>` to batch-queue tasks into the session task list, OR when a document is pointed at as a source of tasks (arg is a file path, or the conversation says a file/doc "has tasks / TODOs / open items to track"). `<items>` may be multiline (one per line), comma-separated, a numbered/`#`-prefixed list, or a file/document path whose actionable items get extracted. Appends to the existing sequence (never clears it), dedups against tasks already present, and stamps each new task with a `#N` number — reusing the source's own numbering when the list is still empty, otherwise continuing the running sequence. Not for single-task tracking mid-work (call TaskCreate directly), not for editing/completing existing tasks (use TaskUpdate).
---

# add-tasks — batch-queue tasks with #N numbering

Turn a batch of items into task-list entries in one shot. **Appends** to whatever is already queued (does not reset the list), **dedups** against existing tasks, and prefixes every new task subject with a running `#N` that continues from the current max.

## When to use

✅ User invokes `/add-tasks <items>` — a batch of things to track
✅ `<items>` is multiline, comma-separated, or numbered / `#`-prefixed
✅ `<items>` is a **file/document path** — read it and extract its actionable items
✅ The conversation points at a document as *having tasks* (e.g. "the open questions in that draft", "this doc's TODOs — queue them")
✅ You want them appended to the existing queue, not a fresh list

## When NOT to use

❌ Tracking one task that surfaced mid-work → call `TaskCreate` directly
❌ Completing / deleting / re-ordering existing tasks → use `TaskUpdate`
❌ Just want to see the queue → use `TaskList`

## Procedure

Create a todo per step and work them in order.

### 1. Read the existing sequence

Call `TaskList` first. Record:
- every existing task **subject** (for dedup)
- the **highest `#N`** already in use across subjects (numbers may sit anywhere in the subject, e.g. `#7 修 schedule bug`)
- whether **any** existing task carries a `#N` at all — this decides the numbering base in step 4

Never assume the list is empty — new tasks always join the existing sequence.

### 2. Parse the input into discrete units

**First decide the input mode:**

- **Document mode** — the argument is a file path (contains a `/`, ends in `.md`/`.txt`/similar, or resolves to a file on disk), OR the conversation points at a specific document as the task source. → go to step 2a.
- **Literal mode** — the argument is the items themselves. → go to step 2b.

If the input mixes both (e.g. "add these two plus whatever's in `draft.md`"), do both and merge before dedup.

#### 2a. Document mode — extract tasks from the file

`Read` the referenced document, then pull out **only actionable work items**:

- checklist entries — `- [ ]` (skip `- [x]`, already done)
- explicit task headings — `Task N:`, `TODO:`, `待辦`, `要做的`, `action items`
- "open questions" / "待拍板" items **only when phrased as work to do** (decide X, confirm Y); skip pure background prose
- numbered work lists under a "next steps" / "剩下" / "尚未" heading

**Do NOT pull in:** "out of scope / 不做" sections, "done / 已交付" items, background/context prose, or design rationale. When unsure whether a line is a task, include it and flag it in the report so the user can prune.

Each extracted item becomes one unit. Note the source doc so the report and description can cite it.

#### 2b. Literal mode — split the argument

Split on whichever delimiter is present:
- **newlines** → one item per non-blank line
- **commas** → one item per comma-separated chunk (when the input is a single line)
- **numbered / `#` list** (`1. …`, `2) …`, `#7 …`) → strip the leading marker; the item is the text after it

Trim whitespace and leading list markers. Drop empty units.

### 3. Dedup against existing subjects

For each parsed item, compare semantically against the existing subjects from step 1:
- **Already present** (same task, ignoring any `#N` prefix and trivial wording differences) → **skip**, keep the existing task untouched. Do not create a duplicate.
- **New** → carry it to step 4.

When unsure whether two are "the same task", prefer treating them as the same (skip) and note it in the report so the user can override.

### 4. Create each new task with a `#N`

**First pick the numbering base:**

- **Existing tasks already carry a `#N`** (list not empty, per step 1) → **continue from the max**. Ignore any numbers the source items carry; running numbers win so the session stays contiguous and never collides.
- **No existing task carries a `#N`** (empty base) **and every parsed item carries an explicit source number** (`task 0` / `task 1`, `#0` / `#1`, `0.` / `1)` …) → **reuse the source numbers verbatim**. If the source starts at `0`, the first task is `#0`. This preserves the doc's own numbering.
- **No existing `#N` and the source is unnumbered** (or only some items are numbered) → **start from `#1`** and run upward.

For each new item, in input order, call `TaskCreate`:
- **subject**: `#N <concise imperative title>` — `N` per the base above (source number, or the next running number). Keep the title short; move detail to the description.
- **description**: the full item text expanded into a clear sentence of what needs doing. In document mode, cite the source (e.g. `來源：docs/…/foo-draft.md §7`) so the task is traceable back to the doc.
- **activeForm**: the present-continuous form (e.g. subject `#3 修 schedule :30 漏畫` → `修 schedule :30 漏畫中`).

One `TaskCreate` call per new item. When continuing a running sequence, numbers are contiguous and never reused, even across separate `/add-tasks` invocations. When reusing source numbers, a skipped duplicate simply leaves its source number uncreated (gaps are fine).

### 5. Report

Summarize concisely:
- **新增 N 筆**: list each as `#N <subject>`
- **跳過 M 筆（已存在）**: list each with the existing task it matched

Keep it tight — the task list itself is the record.

## Notes

- `#N` is a human-facing label inside the subject string; it is independent of the task-list's own internal IDs.
- Numbering is monotonic across the session **once a running sequence exists**: a second `/add-tasks` continues from where the first left off (skipped duplicates do not consume a number).
- Reusing source numbers only ever happens on the **first** batch into an empty list. After that, tasks carry `#N`, so every later `/add-tasks` continues the running sequence regardless of what numbers its source items carry.
- Follow the session's response language for titles/descriptions (this user: 繁體中文 for prose, 原文 for code/identifiers).
