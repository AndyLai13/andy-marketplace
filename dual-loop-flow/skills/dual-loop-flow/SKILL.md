---
name: dual-loop-flow
description: Plan multi-PR feature with vertical-slice + double-loop TDD (a.k.a. outside-in TDD) workflow. Use when feature needs 5+ PRs, AC are frozen, trunk-based env. OPSX / Figma / Jira are used when available and gracefully degraded (never blocked) when absent. Trigger via `/dual-loop-flow <feature-name>` or `/dual-loop-flow <parent-ticket>`.
---

# dual-loop-flow — Vertical Slice + Double-Loop TDD Workflow

Plans and structures a multi-PR feature using vertical slicing (by AC group) with double-loop TDD — a.k.a. **outside-in TDD** (outer acceptance tests drive inward to inner unit tests). Output lands in OpenSpec (`tasks.md`) and Jira sub-tasks **when those integrations are present**; when they (or Figma) are absent the skill degrades gracefully instead of stopping — see §Integration modes.

## When to use

✅ User wants to plan multi-PR feature (5+ PR)
✅ AC are written in Jira and PM-confirmed (frozen or near-frozen)
✅ Trunk-based development environment with feature flag mechanism
✅ User invokes via `/dual-loop-flow` or asks "拆 multi-story feature" / "規劃 multi-PR" / similar

## When NOT to use

❌ Trivial single-PR change
❌ AC not yet finalized (use `/gwt` first)
❌ Feature can be done in 1-2 PRs
❌ Need horizontal slicing or skeleton-style PR #1 — those don't compose with this skill's Foundation-freeze model (see below)

---

## Mandatory workflow rules (non-negotiable within this skill)

This skill enforces **four structural rules**. They are NOT personal preferences — they are requirements of the dual-loop TDD + Foundation-freeze methodology. If your team needs different rules, this skill is the wrong tool; use raw TDD without Foundation-freeze.

### Rule 1: Vertical slicing only (by AC group, NOT tech layer)

❌ **Horizontal slicing** (Domain PR → ViewModel PR → UI PR → integration PR) creates strong dependency chains. PR #N+1 must wait for PR #N to merge. PRs cannot run in parallel.

✅ **Vertical slicing** (each PR = 1 AC group, self-contained outer + production + inner test) lets slice PRs merge in any order. The Foundation PR freezes contract; slice PRs work against the locked contract independently.

**Why structural**：the "main 永遠綠 + slice PR 可亂序 merge" guarantee requires vertical slicing. Without it, the @Ignore-protected Foundation model collapses — each slice would touch the same Domain / ViewModel / UI classes in conflicting ways, requiring serial merge.

**Skill behavior**：in Phase 3, if user proposes horizontal grouping, skill refuses, re-explains, and re-prompts vertical slice plan. No override.

### Rule 2: β-style PR #1 outer tests (full assertion + @Ignore)

❌ **α** (skeleton `@Test fun acN() { TODO() }`) means slice PRs each author their own assertion code. This breaks Foundation-freeze: no locked contract exists; slice PRs can drift on what "the AC means".

❌ **γ** (mixed simple-β + complex-α) creates inconsistent contract coverage; cohesion of "Foundation locks all AC behavior" is lost.

✅ **β** (PR #1 writes complete Given/When/Then assertion code, all `@Ignore("WIP: ...")`) locks contract on Day 1. Slice PRs un-ignore + implement to GREEN against the locked test. If a slice author believes an assertion is wrong, they open a separate PR #1.x to amend — they do NOT modify in their slice PR.

**Why structural**：the @Ignore + Foundation mechanism specifically requires outer test bodies to be **complete and locked** before slicing begins. α / γ defeat this — Foundation no longer freezes anything actionable.

**Skill behavior**：in Phase 4, skill confirms β (no AskUserQuestion choice) and explains the rule. No override.

### Rule 3: IA / entry-form decisions require Figma confirmation (not PRD text alone)

❌ **Decide entry container / location / form factor from PRD wording**. PRD text is structurally ambiguous on IA: "Side Tool Bar 上的 Timer 入口" can mean a standalone STB button, an inline chip row anchored to STB edge, a dropdown from STB, etc. Locking IA from PRD text alone gambles the entire Foundation contract.

✅ **Decide IA only with Figma confirmation, OR mark `[Figma TBD]` with a Final-PR gate**. Style-level TBD (color, drop-shadow, scrim opacity) is fine to placeholder + skin later. IA-level decisions (where the entry lives, what container hosts it, what form factor it takes) MUST be Figma-confirmed before Foundation freezes — or explicitly deferred with a TBD slot in Final PR tasks.

**Why structural**：Foundation locks the outer-test contract. If outer tests assert state of the wrong container (e.g., `FinchSideButtonController` slot 2 when Figma shows widget-panel inline chips), then:
- All slice PRs build production code against the wrong entry model
- Slice tests un-ignore and pass against the wrong assertions → false confidence
- Late discovery → not a Final-PR skin patch; it's a full slice-plan rebuild
- The cost is symmetric to Rule 1: get IA wrong and Foundation-freeze breaks the same way as horizontal slicing breaks it — except worse, because outer tests now actively encode the wrong contract

The classification gate: **does this decision change WHAT element the user taps / WHERE that element lives / WHAT container hosts it?** If yes → IA-level → Figma-required. If no (purely visual skin) → style-level → placeholder OK.

**Skill behavior**：in Phase 2 design.md generation and review, scan every Decision (D1, D2, …) for IA-level content. For each IA Decision:
- If finalized without Figma citation → refuse; require either (a) Figma reference / screenshot citation in the Decision body, or (b) mark `[Figma TBD]` and add a corresponding TBD task to Final PR
- If Figma not yet available → force `[Figma TBD]` form; do NOT finalize from PRD wording

The Phase 0 Q4 (Figma freezing status) feeds this gate: if status is "Partial TBD" or "Not started", Phase 2 MUST surface IA Decisions as Figma-blocked even if user proposes finalizing from PRD.

### Rule 3b: Element Inventory Gate (follow-on to Rule 3 — style/skin is no longer Figma-exempt)

> For every UI surface listed in a Rule 3 IA-level Decision, before the slice PR implementing it merges, the design.md MUST include an **Element Inventory table** with one row per Figma child node (icon, badge, decoration, arc, indicator, text label), each row citing `nodeId` and listing `{geometry | color | size}` as either *Figma-locked*, *Token-mapped*, or *Provisional → Final PR gate task created*. Any element marked `Provisional` MUST generate a checkbox task in the Final PR section (not a comment-only "Design QA tracks separately"). Implementer self-deferred QA does not satisfy this gate.

**Why structural**: Rule 3 alone scoped "Figma-aligned" narrowly to macro IA (entry location / container / form factor) and classified child-node visuals (chip icon, arc geometry, decoration color) as separate "Styling" / "Implementation" categories waived from Figma citation. VB-82 Dashboard chip widget shipped with 5/10/15 chips missing their play button and the arc rendered at wrong geometry/color/structure — never compared pixel-for-pixel to Figma because:
- spec.md only required "four chips: 5 / 10 / 15 / + (icon)" abstractly
- tasks.md asked devs to render an arc without a "match Figma" verify step
- the only D-entry touching chip visuals restricted its scope to the arc's semantic invariant ("purely decorative, MUST NOT animate") not its geometry/color

→ Macro IA correctness is necessary but not sufficient. Every UI surface fingered by an IA-level Decision needs its child-node visuals explicitly classified into one of three states, with `Provisional` forcing a Final PR checkbox.

**Example Element Inventory table** (VB-82 chip widget — the worked counter-example):

| Element | Figma nodeId | Geometry | Color | Size | Status |
|---|---|---|---|---|---|
| `chip_arc` | `8011:23047` | arc start/sweep angle, stroke width | `vsds_sys_color_primary_60` | container 64×64dp | **Figma-locked** |
| `chip_play_button` | `8011:23052` | 12×12dp triangle centered | `vsds_sys_color_on_primary` | 12dp | **Figma-locked** |
| `chip_text` | `8011:23049` | baseline 8dp below center | TextAppearance.VSDS.LabelLarge | — | **Token-mapped** |
| `plus_chip` | `8011:23070` | icon 16dp centered, no arc | `vsds_sys_color_on_surface` | container 64×64dp | **Figma-locked** |

If `chip_play_button` were marked `Provisional`, Final PR `tasks.md` MUST contain `- [ ] Bind chip_play_button geometry to Figma node 8011:23052 (currently provisional)` — not just a code comment.

**Anti-pattern (do NOT accept)**: implementer comments like

> // Geometry is provisional pending the Figma asset Design QA tracks separately

or

> // TODO: Design QA will adjust color in a later PR

These are NOT acceptable deferrals. The words "provisional", "pending Design QA", "tracks separately", "Design QA will adjust" all mean a checkbox task MUST exist in the Final PR `tasks.md` with that element's `nodeId` and the specific deliverable. Implementer self-deferred QA does not satisfy this gate.

**Skill behavior** (Phase 2 follow-on): for each IA-level surface confirmed in Phase 2, immediately call `mcp__claude_ai_Figma__get_metadata` on that node, enumerate every child node, and emit a draft Element Inventory table into `design.md`. If Figma MCP is unavailable, **degrade rather than block** (see §Integration modes): take the Element Inventory rows from the user manually, or mark the whole surface `Provisional` + Final-PR gate task. What never degrades: shipping a surface with *no* child-node classification at all, or finalizing IA from PRD text — both are forbidden regardless of Figma availability.

### Rule 4: UI is two tracks — interaction is slice-tested, visual is slice-verified (NOT TDD'd, NOT Final-PR-deferred)

❌ **Treat "the UI" as one undifferentiated thing that gets retrofitted after the dual loop closes.** The skill's outer tests deliberately assert behavior at the ViewModel abstraction (Rule 3: `viewModel.onPresetSelected(...)`, not `R.id.timer_stb_button`). That keeps the outer ring robust against IA churn — but it also means the actual View↔ViewModel wiring and the on-screen visual have **zero test driving them inside the loop**. Result: Domain→VM gets chiselled out, the loop goes green, and the real UI is shoved to "later" → shipped as a separate retrofit pass.

✅ **Split UI into two tracks, both delivered inside the slice that owns the surface:**

| UI half | Testable? | This skill's track |
|---|---|---|
| **Interaction / behavior** (tap the real element → VM call / state change) | ✅ yes | A **View-binding integration test** (Robolectric/Espresso) — the missing middle ring between outer-acceptance (VM) and inner-unit. Lives in the slice. |
| **Visual / skin** (geometry, color, padding, animation, layout) | ❌ no — asserting it = copying the layout into a test | **Visual verification** against Figma + the Rule 3b Element Inventory. NOT an assertion test. Done in-slice via screenshot/manual compare, not deferred wholesale to Final PR. |

**Why structural**: in dual-loop TDD the outer and inner rings are bridged by an **integration test** — for a Web endpoint that's the HTTP-layer test; for Android UI that's the View↔VM binding test — and **visual UI must not be TDD'd** (asserting padding/color/layout = copying the layout into a test, zero protection value). Omit track A and the UI wiring is untested; omit track B's slice-scoping and the visual slides to a Final-PR catch-up bucket. Either omission reproduces 「做完才回頭補 UI」.

**Skill behavior**:
- Phase 2 Element Inventory enumeration covers **every UI surface any slice will touch** — not only IA-Decision surfaces (see Phase 2).
- Phase 6 slice-task template carries UI deliverables: a View-binding integration test + the surface's visual implementation, gated by a slice-entry "visual frame in hand" precondition. Slice DoD is not satisfied by VM-level outer green alone.
- Final PR receives **only genuine Figma-TBD** (asset truly not yet in Figma at slice time), never the bulk of UI work.

### Asset provision policy (when to hand over Figma / screenshots)

UI artifacts are provided in **stages, by layer** — not all up front, and not lazily at the end. Two layers, two timings, two failure modes:

| Artifact | Locks | Latest acceptable moment | Lives in |
|---|---|---|---|
| **IA-level Figma** (entry location / container / form factor) | Foundation outer-test contract | **Phase 2**, before Foundation freezes (Rule 3) | design.md Decision body citation (link / frame X confirmed YYYY-MM-DD) |
| **Element Inventory child nodes** (icon/arc/badge/text nodeIds) | Rule 3b classification table | **Phase 2**, via `get_metadata` | design.md Element Inventory table (`nodeId` column) |
| **Per-slice visual frame + screenshot** (the surface's pixel detail) | that slice's visual fidelity | **before that slice PR starts** (just-in-time; for kickoff-complete projects, batched at Phase 2) | slice-entry precondition + repo `assets/` path cited in design.md |
| **Style micro-tweaks** (color / shadow / scrim) | skin only | may defer to **Final PR** (placeholder token first) | Final PR genuine-TBD checkbox |

**The placeholder-on-unavailable rule (style-level ONLY):**
- **IA-level missing → BLOCK, never placeholder.** IA wrong = wrong contract = full slice-plan rebuild (Rule 3). For kickoff-complete projects this should never recur after Phase 2; if it does, Phase 2 was incomplete → stop and re-do Phase 2, do not guess from PRD.
- **Style-level missing during dev + author unreachable → placeholder is allowed *only under the standing pre-authorization captured in Phase 0 Q5*.** A normal interactive session blocks on `AskUserQuestion` (there is no real "N-minute auto-timeout"); the "wait ~10 min then placeholder" pattern only holds when the user granted it up front as a standing rule (Phase 0 Q5), or in an autonomous `/loop`. When pre-authorized: drop a VSDS-token / provisional placeholder, mark the Element Inventory row `Provisional`, and open a Final-PR backfill checkbox carrying the `nodeId`. Never block on the user for style.
- **Every placeholder MUST be tracked** (Element Inventory `Provisional` row + Final-PR `- [ ]` checkbox with `nodeId`). A bare code comment (`// provisional pending Figma`) is NOT a valid deferral (Rule 3b) — that omission is exactly how VB-82 chips shipped missing their play button.

### What about α / γ existing in the playbook?

`~/.claude/docs/feature-pr-slicing-playbook.md` §4 documents α / γ for completeness — they're valid TDD variants in other contexts (multi-dev teams with shifting AC, learning contexts where each dev writes own tests). **They don't compose with this skill's Foundation-freeze model**, so this skill enforces β only.

---

## Integration modes (OPSX / Figma / Jira — detect, then degrade gracefully)

This skill uses three external integrations. **Detect each in Phase 0; if present, use it; if absent, degrade — never hard-stop because a tool is missing.** The dual-loop + vertical-slice methodology (the four Rules above) is fully tool-independent; OPSX / Jira / Figma are delivery vehicles, not the method.

**Two kinds of "missing" — do NOT conflate them:**
- **Tool absent** (no `openspec` CLI / Jira not connected / Figma MCP unauthenticated) → degrade and keep going. ✅ This is the whole point of modes.
- **Information absent** (IA not yet frozen in Figma) → this is Rule 3's gate, NOT a tool problem. Degrade to `[Figma TBD]` + Final-PR gate task; the plan still completes. ❌ **NEVER degrade this into "finalize IA from PRD text"** — that is the VB-82 failure mode Rule 3 exists to prevent. Less Figma = more `TBD`, never more guessing.

| Integration | Detect (Phase 0) | Present → | Absent → degrade to |
|---|---|---|---|
| **OPSX** (OpenSpec) | `openspec --version` ok / repo has `openspec/` | scaffold + `openspec validate` + propose→apply→archive lifecycle | write artifacts under `docs/plans/<date>-<feature>/`; skip every `openspec new/validate/archive`; omit the `/opsx:archive` reminder |
| **Jira** | Atlassian MCP reachable **and** parent ticket supplied | Phase 1 `getJiraIssue`; Phase 7 create N+2 sub-tasks + backfill keys | AC from user-pasted text / local file; Phase 7 emits a **sub-task plan table** inside `tasks.md` (no ticket creation); backfill column = `Jira: N/A` |
| **Figma** | Figma MCP authenticated **and** frames exist | IA confirmation + `get_metadata` auto Element Inventory | IA Decisions → all `[Figma TBD]` + Final-PR gate; Element Inventory → user-supplied rows OR whole surface `Provisional` + Final-PR gate. **Still never finalize IA from PRD (Rule 3).** |

**Surface the degradation, never hide it:**
- **Phase 0** prints a one-line capability matrix (e.g. `OPSX=on · Jira=off · Figma=TBD`) before asking questions, so the user knows which paths are degraded up front.
- **Phase 8** output prints a **degradation manifest** — every step downgraded and everything turned `TBD` — so "not done because tool absent" is never silently read as "done".

---

## Reference materials (read on demand)

**Global (skill 內建依賴)**：
- **Playbook (theory)**: `~/.claude/docs/feature-pr-slicing-playbook.md` — full methodology, decision trees, failure modes
- **SOP (checklist)**: `~/.claude/docs/feature-sop-checklist.md` — Phase 1-7 actionable steps

**Project-specific (專案 docs，cross-environment portable)**：
- **Testing conventions**: `<project-root>/docs/testing-conventions.md` — TestCase 三層分流、manual 元件清單、TestCase folder 位置
- **Project CLAUDE.md**: `<project-root>/CLAUDE.md` — 該專案的 testing pipeline 摘要 + 連結到 testing-conventions.md

**Personal memory cross-refs (optional, 純歷史脈絡)**：
- `[[feedback-pr-vertical-slicing]]`、`[[feedback-outer-test-full-assertion]]` — 規則制定原因記錄（規則本身已內建於本 skill §Mandatory workflow rules）
- `[[opsx-with-double-loop-tdd]]` — 歷史脈絡（被本 skill supersede）

> **Skill 跨環境 portability**：global docs + project docs 即可運作；β + 縱切兩條核心規則已強制內建（不靠 memory）。memory 只是個人 rationale 紀錄。

## Workflow

Walk user through phases 0-8. Use `AskUserQuestion` at decision points. Use `Edit` / `Write` for file changes. Use Jira MCP tools for sub-task creation.

### Phase 0: Scope intake

**First**: read project's `docs/testing-conventions.md` (if exists) to learn TestCase folder location, manual components list, and pipeline conventions for this codebase. If not exists, skill works but `[manual]` artifact checks will require user manual input.

**Then detect integrations** (see §Integration modes): probe `openspec --version` / `openspec/` dir (OPSX), Atlassian MCP reachability + whether a parent ticket is supplied (Jira), Figma MCP auth (Figma). Record `OPSX / Jira / Figma` capability flags and **print the capability matrix to the user** (e.g. `OPSX=on · Jira=off · Figma=TBD`) before the questions. Each absent integration switches its phases to the degraded path — never stop because one is missing.

Use `AskUserQuestion` (5 questions; skip / adapt any that a missing integration makes moot):

1. **Parent ticket key** (e.g., "VB-200") — *Jira mode*. If Jira absent: skip; use the feature name as the plan id and collect AC in Phase 1 instead.
2. **Feature name** (kebab-case, e.g., `add-foo`)
3. **Multi-story status**:
   - Standalone (single Story)
   - Multi-story: 第幾支 + 依賴哪些前置 Story Foundation PR
4. **Figma freezing status** — 分兩層問，因為 IA 與 styling 的 TBD 容忍度不同（見 Rule 3）：
   - **IA 層**（entry 位置 / 容器形態 / form factor）：Fully frozen / Partial TBD (列出哪些 AC 涉及) / Not started
   - **Styling 層**（顏色 / 字級 / drop-shadow / icon 細節）：Fully frozen / Partial TBD / Not started

   ⚠️ 若 **IA 層為 Partial TBD or Not started** → Phase 2 design.md 必須把對應 Decision 標 `[Figma TBD]`，不得從 PRD 文字 finalize（Rule 3）。Styling 層 TBD 可接受 placeholder + Final PR 補完。
5. **Style-asset standing authorization** — 一條 standing rule，governs 所有後續 slice（餵 Asset provision policy）：
   - 「開發中若缺 **style 層** 素材且你當下不在 → 預先授權放 placeholder + 開 Final PR backfill checkbox（不卡你）」：Yes / No
   - ⚠️ 此授權**僅限 style 層**。**IA 層一律 block 等你確認**（Rule 3 / Rule 4 Asset provision policy），不在此授權範圍。
   - 記錄答案；Phase 6 slice-entry gate 與 dev 期 placeholder 行為都依此判斷。

If AC 未過 `/gwt`：instruct user to run `/gwt` first; do not proceed.

### Phase 1: AC integrity check

Get the AC, by mode:
- **Jira present**: `getJiraIssue` (markdown format, fields=`["summary", "customfield_12203"]`).
- **Jira absent**: ask the user to paste the AC or point to a local AC file (e.g., output of `/gwt`); read that. Record the source path in the plan for traceability.

Compute outer-test count:
- AC count: N
- Outer tests: Σ scenarios (count multi-scenario invariant ACs)

Show user the breakdown table before proceeding.

### Phase 2: Change scaffold (OPSX present) / plan-doc scaffold (OPSX absent)

**OPSX present** — if `openspec/changes/*-<feature>` doesn't exist:
- Run `openspec new change "<feature>"`
- Rename with date prefix: `mv openspec/changes/<feature> openspec/changes/YYYY-MM-DD-<feature>` (use `date +%Y-%m-%d`)
- Generate `proposal.md` / `design.md` / `specs/<capability>/spec.md` (if user hasn't done /opsx:propose)
- If already exists, read existing artifacts to understand context.

**OPSX absent** — create `docs/plans/YYYY-MM-DD-<feature>/` with `design.md` (and later `tasks.md`); skip every `openspec` command (no `new`, no `validate`). No `proposal.md` / `spec.md` delta tracking — note in the manifest that the OpenSpec spec-lint + lifecycle is not available.

Everything below in this phase (IA gate, Element Inventory) is **identical in both modes** — it operates on `design.md` regardless of where that file lives.

**IA gate (Rule 3 enforcement)** — before validating, scan `design.md` Decisions for IA-level content:

- For each `### D<N>:` Decision, classify the subject:
  - **IA-level** if the Decision changes *where the entry lives, what container hosts it, or what form factor it takes* (e.g., "Timer 入口 — STB slot 2 vs widget panel inline chip row", "Floating clock close button — top-right corner vs floating handle")
  - **Style-level** if it's purely visual skin (color, drop-shadow, scrim opacity, icon polish)
- For every IA-level Decision:
  - Must cite Figma (screenshot path / Figma link / "Figma frame X confirmed YYYY-MM-DD") in the Decision body, OR
  - Must be marked `**[Figma TBD]**` with a corresponding TBD task already drafted into Final PR section of `tasks.md`
- If a Decision is finalized from PRD text alone (e.g., "**為什麼**：PRD 寫明 …" with no Figma citation) → flag to user, refuse to proceed until either (a) Figma confirmation added, or (b) Decision rewritten as `[Figma TBD]`

The trap this prevents: PRD wording is structurally ambiguous on IA. "Side Tool Bar 上的 Timer 入口" can mean a standalone STB button, an inline chip row anchored to STB edge, or a dropdown — Figma is the only source of truth for which.

**Element Inventory gate (Rule 3b + Rule 4 enforcement)** — enumerate Element Inventory for **every UI surface any slice will touch**, not only IA-Decision surfaces:

- Surfaces in scope = (a) every IA-level surface that survived the Rule 3 gate, PLUS (b) every other UI surface a planned slice renders or mutates (chips, panels, dialogs, overlays, list rows…). Rule 4: a visual that no IA Decision happens to mention still needs child-node classification, otherwise it silently slides to a Final-PR retrofit.
- For each surface, call `mcp__claude_ai_Figma__get_metadata` on its Figma node to enumerate every child node (icon, badge, decoration, arc, indicator, text label).
- For each child, emit one row in `design.md` Element Inventory table: `Element | nodeId | Geometry | Color | Size | Status`. Status MUST be one of: **Figma-locked**, **Token-mapped**, or **Provisional**.
- Every `Provisional` row → draft a corresponding `- [ ] ...` checkbox into the Final PR section of `tasks.md` (Phase 6 will pick it up). A code comment like "Design QA tracks separately" or "provisional pending Figma asset" is NOT a substitute — reject and force a checkbox task.
- **If Figma MCP is unavailable (unauthenticated / no frames) → degrade, do NOT block** (see §Integration modes): either ask the user to supply the Element Inventory rows manually, or mark the whole surface `Provisional` + a Final-PR gate task, and turn every IA-level Decision for that surface into `[Figma TBD]`. The plan completes with those visuals explicitly flagged. ❗ The one thing that never degrades: do NOT finalize IA from PRD text to "fill the gap" (Rule 3).

**Asset batch (kickoff-complete projects)** — when Phase 0 Q4 reports Figma largely frozen at kickoff: pull all confirmed assets here in one batch (IA Figma + every surface's child nodes + screenshots into repo `assets/`), so each slice's visual frame is already in hand. The per-slice slice-entry gate (Phase 6) then degrades to a cheap precondition check rather than a stop-and-wait. Genuinely-unavailable visuals → `Provisional` + Final-PR checkbox (genuine TBD).

Validate (**OPSX present only**): `openspec validate "<change-name>"`. OPSX absent → skip; rely on the manual IA/Element-Inventory gates above (note the missing spec-lint in the manifest).

### Phase 3: Slice AC into vertical groups (Rule 1 enforced)

⚠️ **Vertical slicing only** — see Mandatory workflow rules §Rule 1.

Propose a slicing plan, then `AskUserQuestion` to confirm:

- Show AC list with auto-suggested grouping **by functional cohesion**
- Each slice: 5-10 outer tests ideal; > 15 warn + suggest split
- 如有 Figma TBD：標出哪 slice 涉及，placeholder layout + Final 補完

**If user proposes horizontal grouping** (by Domain / ViewModel / UI layer)：refuse, re-explain Rule 1, re-prompt. Do NOT proceed with horizontal — that breaks Foundation-freeze.

Allow user to adjust **vertical** grouping interactively (slice boundaries, AC↔slice mapping).

### Phase 4: Confirm β style (Rule 2 enforced — no choice)

⚠️ **β is mandatory** — see Mandatory workflow rules §Rule 2. No AskUserQuestion.

Inform user (do not ask):

> 「PR #1 採 β 風格：完整 outer test assertion + @Ignore。Slice PR 只 un-ignore + 寫 production，不改 assertion。
> 此為 skill 強制規則（α / γ 與 Foundation-freeze 模型不相容）。」

Reference playbook §4 if user asks "why not α / γ" — explain α/γ exist as alternatives but this skill enforces β.

### Phase 5: Flag flip strategy

If single-story:
- Single flag, Final PR flips

If multi-story (from Phase 0):
- `AskUserQuestion`: Option A / B / C / D (playbook §12)
- Default to **Option B** (single master flag, last Story Final flips)

### Phase 6: Generate tasks.md

Use `Write` to generate `tasks.md` — path by mode: **OPSX present** → `openspec/changes/<date>-<feature>/tasks.md`; **OPSX absent** → `docs/plans/<date>-<feature>/tasks.md` (alongside the Phase 2 `design.md`).

**Header**:
- 雙循環 TDD + 縱切策略 explanation
- AC ↔ slice 對應 table
- 相依性矩陣 table (with TBD Jira keys)
- 依賴圖 (ASCII)

**Foundation Task (Task 1)**:
- Skeleton interfaces / domain / fakes
- Cross-story refactor (if multi-story)
- All outer @Test methods with **β-style full assertion** + `@Ignore("WIP: <ticket> §N, unblock at §M")`
- BuildConfig.<FLAG> = false
- Test class: `<Feature>AcceptanceTest.kt` with AC↔test mapping comment

**Slice Tasks (Task 2..N+1)**:
- Each `## Task N` header gets blockquote: `> **依賴**: ... | **阻擋**: ... | **可並行**: ... | **Jira**: TBD`
- **Slice-entry precondition (Rule 4 / Asset provision policy)**: 該 slice 的視覺 frame + 截圖到位（kickoff-complete 專案 Phase 2 已 batch；若缺 → IA 層 block、style 層依 Phase 0 Q5 standing 授權 placeholder）
- Production code + un-ignore subset of outer tests + inner test
- **UI deliverables (Rule 4)** — for any slice that renders/mutates a UI surface:
  - **View-binding integration test** (Robolectric/Espresso)：點真實 view → 斷言 VM call / observable state（補 outer-VM 與 inner-unit 之間缺的整合層）
  - **Visual implementation** 對齊該 surface 的 Element Inventory（非 assertion 測試；screenshot / 手動 compare to Figma node）
- 跨 Story 動到 (if any)
- **Slice DoD (Rule 4)**：outer(VM) green **且** View-binding 整合測試 green **且** 該 surface Element Inventory 無 `Provisional` 殘留（除 genuine Figma-TBD）。VM-level outer green 單獨 **不算** slice 完成。

**Final Task (Task N+2)**:
- Multi-story coordination check (git log verify prerequisites merged)
- **Genuine Figma-TBD backfill only** — assets truly unavailable at slice time（每條帶 `nodeId` + deliverable）。⚠️ Final PR **不是 UI catch-up bucket**；UI 的主體（binding test + 視覺）在各 slice 內已交付（Rule 4）。
- Flag flip per Phase 5 strategy
- All outer GREEN verify
- Smoke test
- **Archive reminder (OPSX present only)** — last checkbox of the Final Task: `- [ ] feature ship 後跑 /opsx:archive 歸檔此 change`（multi-story: 只有 LAST story 的 Final Task 帶此項）。這是規劃尾端的固定收尾，避免使用者忘記歸檔。OPSX absent → omit (no archive lifecycle).

Validate (**OPSX present only**): `openspec validate "<change-name>"`. OPSX absent → skip.

### Phase 7: Create Jira sub-tasks + backfill

**Jira absent** → skip ticket creation entirely. Instead emit a **sub-task plan table** into `tasks.md` with one row per Task (Foundation + slices + Final): `PR # | Slice name | 對應 AC | 依賴/Merge 順序 | Outer 點亮數`. Backfill column stays `Jira: N/A`. The user can create the tickets later from this table. Then jump to Phase 8.

**Jira present** — for each Task (Foundation + slices + Final, total N+2):

Use Jira MCP `createJiraIssue` with:
- `projectKey`: derived from parent ticket
- `issueTypeName`: "Sub-task"
- `parent`: parent ticket key
- `assignee_account_id`: from Phase 0
- `summary`: `<ticket> PR #N — <Slice Name> (AC...)`
- `description` (markdown), at least含:
  - 內容範圍
  - Merge 順序 / 依賴 (含跨 Story 如有)
  - 對應 AC
  - Outer test 點亮數
  - 回指 `openspec/changes/<name>/tasks.md` 對應 Task
  - 跨 Story 動到 (如有)
- `contentFormat: "markdown"`, `responseContentFormat: "markdown"`

Collect returned keys into a per-Task map (Task N → VB-XXX).

**Backfill Jira keys to tasks.md** — CRITICAL discipline:
- ⚠️ Do NOT use `replace_all` — multiple Task headers might collide
- Use per-task explicit `Edit` (one Edit per Task header + one for matrix table)
- After all replacements, `grep "Jira.*TBD"` to verify no leftover

### Phase 8: Final verification + report

- `openspec validate "<change-name>"` pass — **OPSX present only**; absent → skip
- `grep "Jira.*TBD" tasks.md` returns nothing — **Jira present only**; absent → confirm the sub-task plan table is complete instead
- Report to user:
  - tasks.md path (OPSX `openspec/changes/...` or plan-doc `docs/plans/...`)
  - **Jira present**: list of N+2 sub-tasks created · **Jira absent**: pointer to the sub-task plan table
  - **Degradation manifest** — for every absent integration, list what was downgraded and everything turned `TBD` (e.g. "Figma off → 3 IA Decisions = [Figma TBD] + Final-PR gates; OPSX off → no spec-lint/archive; Jira off → sub-tasks in table, not created"). Empty manifest if all three present.
  - Friction points encountered (if any unexpected)
  - Next step: "ready to start PR #1 Foundation?"
  - Closing reminder (**OPSX present only**): feature ship（最後 Final PR merge + flag flip）後跑 `/opsx:archive` 歸檔此 change，別忘記

## Discipline / Constraints (enforce when running)

### β style PR #1 outer tests (enforced rule — see Mandatory workflow rules §Rule 2)

- PR #1 includes all outer `@Test` with full Given/When/Then assertion code
- All `@Ignore("WIP: <ticket> §N, unblock at §M")` with explicit reason
- Test only **user-observable state**, never mock implementation details
- If slice finds assertion wrong: open PR #1.1 to fix test, do NOT hack production code

### Acceptance test naming

- Class: `<Feature>AcceptanceTest.kt` (e.g., `TimerSettingAcceptanceTest`)
- Method: `acN_briefName()` or `acN_subName()` for multi-scenario
- Class-top AC↔test mapping comment for QA / PM traceability

### UI two-track delivery (enforced rule — see Mandatory workflow rules §Rule 4)

- UI is split: **interaction** → slice-level View-binding integration test (testable); **visual** → slice-level verification against Element Inventory (NOT an assertion test).
- Both halves ship **inside the slice** that owns the surface. A slice is not done on VM-level outer green alone (see Phase 6 Slice DoD).
- Final PR carries only genuine Figma-TBD, never the bulk of UI — Final PR is not a UI catch-up bucket.
- If a slice author is tempted to "build UI later" → reject; the binding test + visual belong in this slice.

### Asset provision (see Mandatory workflow rules §Asset provision policy)

- IA-level asset missing → **block**, never placeholder; re-do Phase 2, do not guess from PRD.
- Style-level asset missing + author unreachable → placeholder **only** under the Phase 0 Q5 standing authorization; otherwise block and ask.
- Every placeholder = Element Inventory `Provisional` row + Final-PR `- [ ]` checkbox with `nodeId`. Bare code comments are not valid deferrals (Rule 3b).

### Multi-story coordination

- Only LAST story's Final PR flips master flag
- Middle stories' Final PRs verify outer green only
- Cross-story refactor happens in target Story's Foundation PR (not in slice)
- Final PR has explicit git log check for prerequisite Story merges

### Figma TBD (two tiers — see Rule 3)

**Style-level TBD** (acceptable to placeholder + skin in Final PR):
- Color, font size, drop-shadow, scrim opacity, icon polish, animation curve
- Outer tests assert behavior/data, not styling
- Slice uses placeholder VSDS tokens
- Final PR receives Figma confirmation + adjusts layout / drawables

**IA-level TBD** (must NOT be finalized from PRD text — Rule 3):
- Entry location (which container hosts it: STB / dashboard / widget panel / overlay)
- Entry form factor (standalone button / inline chip row / dropdown / popup)
- Container hierarchy (top-level vs nested, sibling vs child of which element)
- ❗ If Figma not confirmed → Decision marked `[Figma TBD]`, Final PR has explicit gate task; outer tests should assert *behavior at the right abstraction* (e.g., `viewModel.onPresetSelected(...)`) rather than coupling to specific container (e.g., `R.id.timer_stb_button`)
- Catching IA wrong in Final PR = slice plan rebuild, not a skin patch

### Slice size

- 5-10 outer tests per slice: ideal
- 10-15: tolerable if functionally cohesive
- > 15: warn user + suggest split

### Backfill safety

- NEVER `replace_all` when backfilling Jira keys to tasks.md
- Per-Task `Edit` with explicit before/after strings
- Verify with `grep "Jira.*TBD"`

## Common friction (recognize + handle)

| Friction | How to handle |
|---|---|
| User wants to skip Foundation PR | Refuse; Foundation locks contract for parallel slices |
| Slice boundary by tech layer (domain / VM / UI) | Reject (Rule 1 violation); insist on AC-group cohesion |
| User asks for α / γ style | Reject (Rule 2 violation); explain Foundation-freeze incompatibility |
| Outer test mocks implementation | Refactor to state-based assertions |
| AC not frozen | Stop; ask user to /gwt + PM confirm first |
| Figma fully missing | Warn; suggest at least confirm AC behavior before slicing |
| IA Decision finalized from PRD text without Figma | Reject (Rule 3 violation); require Figma citation or `[Figma TBD]` + Final PR gate task |
| User insists "PRD 字面寫了 X 入口" | Re-explain Rule 3: PRD text is structurally ambiguous on IA; Figma is the SoT for entry placement / container / form factor |
| IA surface 沒附 Element Inventory table | Reject (Rule 3b violation); call `get_metadata` on the Figma node, enumerate child nodes, classify each as Figma-locked / Token-mapped / Provisional |
| Implementer 留 comment「Design QA tracks separately」/「provisional pending Figma asset」 | Reject (Rule 3b violation); 該 element 必須在 Final PR `tasks.md` 開 checkbox task 標明 `nodeId` 與 deliverable，code comment 不是合法 deferral |
| Figma MCP 未認證但要做 IA surface | **Degrade, don't block** (§Integration modes)：人工提供 Element Inventory 列 或整面標 `Provisional` + Final-PR gate；該 surface 的 IA Decision 全轉 `[Figma TBD]`。唯一不可降級：從 PRD 文字 finalize IA（Rule 3）|
| OPSX (`openspec`) 不存在 | Don't block；產物寫 `docs/plans/<date>-<feature>/`，跳過 `openspec new/validate/archive`；manifest 註明少了 spec-lint + archive lifecycle |
| Jira / Atlassian MCP 不可用 | Don't block；AC 改由使用者貼上 / 本地檔；Phase 7 在 tasks.md 出 sub-task 計畫表（不建 ticket）；backfill 填 `Jira: N/A` |
| UI 被當成單一物、留到雙循環跑完才回頭補 | Reject (Rule 4 violation); 拆兩軌：互動 → slice 內 View-binding 整合測試；視覺 → slice 內 Element Inventory 驗收。Final PR 只收 genuine TBD |
| Slice 只靠 VM-level outer green 就喊完成 | Not done (Rule 4 Slice DoD); 要求 binding 整合測試 green + 視覺對齊 Element Inventory 才算完 |
| 開發中缺 style 素材、使用者當下不在 | 若 Phase 0 Q5 有 standing 預先授權 → placeholder + Provisional 列 + Final backfill checkbox(`nodeId`)；否則 block 等使用者 |
| 開發中缺 IA 素材 | Block；回 Phase 2 重做，絕不 placeholder（Rule 3 / Rule 4 Asset provision policy） |
| Multi-story but unclear which Story you are | Stop; ask Phase 0 question 3 again |

## Output format

After Phase 8, summarize:

```
## dual-loop-flow — <feature> 規劃完成

能力：OPSX=on/off · Jira=on/off · Figma=on/TBD/off

| 產出 | 位置 |
|---|---|
| tasks.md | openspec/changes/<date>-<feature>/tasks.md  *(OPSX off → docs/plans/<date>-<feature>/tasks.md)* |
| Jira sub-tasks | VB-XXX ~ VB-YYY (N+2 條，assignee = ...)  *(Jira off → tasks.md 內 sub-task 計畫表)* |
| 風格 | β / α / γ |
| Flag flip | Single / Option A/B/C/D |
| 跨 Story 依賴 | (列出) 或 — |
| 降級 manifest | (Figma/OPSX/Jira 缺席而降級或轉 TBD 的項目) 或 — 全部到位 |

下一步：實作 PR #1 Foundation 嗎？
（OPSX on 時收尾提醒：所有 PR merge + flag flip ship 後，記得跑 `/opsx:archive` 歸檔此 change）
```

## Related skills

- `/opsx:propose` — 先跑此產出 OpenSpec proposal/design/spec
- `/opsx:apply` — Phase 6 之後實作各 Task
- `/opsx:archive` — feature ship 後歸檔
- `/gwt` — AC 寫成 Rule + violation scenarios（Phase 0 前置）
