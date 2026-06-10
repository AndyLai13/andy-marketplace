# andy-marketplace

Andy Lai's personal [Claude Code](https://claude.com/claude-code) plugin marketplace.

Opinionated skills for **Outside-In TDD** (a.k.a. **Double-Loop TDD** / **BDD + TDD**) workflows, Android test conventions, AC rewriting, and branch hygiene.

> 中文版說明在文件下半部 → [跳到中文](#中文版)

---

## English

### What's in here

One plugin (`toolbox`) bundling seven skills. The headline skill is `dual-loop-flow` — a structured workflow for planning multi-PR features using **vertical slicing + Double-Loop TDD**.

```bash
# Add this marketplace (local path)
/plugin marketplace add ~/andy-marketplace

# Or after pushing to GitHub
/plugin marketplace add https://github.com/AndyLai13/andy-marketplace

# Install the toolbox plugin
/plugin install toolbox@andy-marketplace
```

After install, every skill is namespaced under `toolbox:`.

| Skill | Purpose | Invoke |
|---|---|---|
| [`dual-loop-flow`](#dual-loop-flow--double-loop-tdd--outside-in-tdd-planner) | Plan multi-PR features with **Outside-In TDD** (vertical slice + Foundation-freeze) | `/toolbox:dual-loop-flow` |
| [`gwt`](#gwt--given-when-then-ac-rewriter) | Rewrite/review Acceptance Criteria into **BDD-style Given-When-Then** | `/toolbox:gwt` |
| [`test-funnel`](#test-funnel--ac--unit--instrumented--manual-triage) | Turn AC into a TC-table doc and triage each case into **unit / instrumented / manual** — then write the tests | `/toolbox:test-funnel` |
| [`android-testing`](#android-testing) | Android `:app` testing conventions (MockK / Turbine / Robolectric / JUnit 4) | `/toolbox:android-testing` |
| [`cleanup-merged-branch`](#cleanup-merged-branch) | End-of-branch cleanup: sync main, verify merge (incl. squash via `gh`), delete local | `/toolbox:cleanup-merged-branch` |
| `start-task` | Pre-PR convergence pipeline: worktree → `/review-pr` loop → spotless → commit tidy | `/toolbox:start-task` |
| `roborazzi-figma` | Figma↔Android pixel-match loop: render → diff → fix via Roborazzi, then lock as CI golden | `/toolbox:roborazzi-figma` |

`dual-loop-flow` references `/toolbox:gwt` in its AC-prep step — both ship in the same plugin, so installing `toolbox` gives you the full chain.

---

### `dual-loop-flow` — Double-Loop TDD / Outside-In TDD planner

The core methodology skill. Plans a multi-PR feature using **vertical slicing by AC group** and **Double-Loop TDD** (a.k.a. **Outside-In TDD**, the **BDD + TDD** double-loop):

- **Outer loop (BDD / acceptance)** — failing acceptance test written from the AC drives the slice. Lives in the outer ring of the [Double-Loop TDD diagram](https://coding-is-like-cooking.info/2013/04/outside-in-development-with-double-loop-tdd/): *Write a failing acceptance test → make it pass*.
- **Inner loop (TDD / unit)** — classic red/green/refactor at the unit level drives the implementation needed to turn the outer test green.

> "Outside-In" = start from the outermost user-facing assertion (AC → acceptance test), then push inward through unit tests. The outer test stays red until the slice is functionally complete; inner tests cycle red→green→refactor many times inside it. This is the **double loop**.

**What the skill produces**

For a feature with N AC groups, it plans:

1. **PR #1 — Foundation PR (β-style)**: writes **complete Given/When/Then acceptance test bodies** for every AC, all marked `@Ignore("WIP: ...")`. This freezes the contract on Day 1.
2. **PR #2…N — Slice PRs (one per AC group)**: each PR un-ignores its outer test, drives the implementation inward via inner-loop TDD (Domain → ViewModel → UI as needed), and lands self-contained.
3. **Final PR**: polish, deferred design TBDs, flag flip.

Because outer tests are **locked at Foundation**, slice PRs can land in any order and `main` stays green throughout — that's the Foundation-freeze guarantee.

**Why the rules are strict (no override)**

The skill refuses horizontal slicing, α-style skeleton outer tests, and IA decisions inferred from PRD prose. Each rule is structural — break it and the Double-Loop / Foundation-freeze model collapses (see the skill body for the long-form reasoning).

**Phase 8 manual-test gate**: every slice PR finalizes a manual test case (written to `test_case/<slice>.md`) before the slice is considered done — bridges the gap until UI/E2E automation catches up.

**Integration modes** — OPSX / Figma / Jira are used when available, gracefully degraded when absent. The skill never blocks on an external tool; it switches modes and keeps planning.

**Invoke**

```
/toolbox:dual-loop-flow <feature-name>
/toolbox:dual-loop-flow <parent-jira-ticket>
```

**When to use**: feature needs 5+ PRs, AC are frozen (or near-frozen), trunk-based dev with a feature flag.
**When NOT to use**: trivial single-PR change, AC not yet written (run `/toolbox:gwt` first), feature fits in 1–2 PRs.

---

### `gwt` — Given-When-Then AC rewriter

Rewrites or reviews acceptance criteria into concrete **BDD-style Given/When/Then** form — the prerequisite shape for the outer-loop acceptance tests in **Outside-In TDD**.

Output is written to `test_case/*.md` locally (no Jira write-back). Pairs directly with `dual-loop-flow`: tighten AC with `gwt` first, then plan PRs with `dual-loop-flow`.

```
/toolbox:gwt
```

---

### `test-funnel` — AC → unit / instrumented / manual triage

Turns a feature's AC/PRD into a **TC-table document** and runs each sub-case through a **test funnel**: default everything to "manual", then pull it down — pure logic sinks to a **unit** test (the regression backstop), manual verifications that can be automated rise to an **instrumented** (real-device) test, and only genuine visual/perception checks stay **manual**. It then **actually writes the tests** — delegating unit naming/stack to `android-testing` and using a bundled instrumented harness (`ActivityScenario` for non-exported Activities, gesture injection, settle-gating to avoid flake).

The `✅` (instrumented) bar is **path-based, not value-based**: instrumented is reserved for what unit/Robolectric structurally can't reach — real animation, real gesture, real IPC, real lifecycle/looper-async — even when a pure unit test also covers the value in isolation.

Conditionally calls `/toolbox:gwt` when the AC aren't organized into GWT yet; otherwise consumes the existing `test_case/*.md`. The TC-table format and instrumented harness are extracted from the canonical `edu-vbos-finch` Freezer feature (VB-579 / VB-882).

```
/toolbox:test-funnel <jira-key>
/toolbox:test-funnel "<pasted AC>"
```

---

### `android-testing`

Encodes the actual testing conventions of the project this skill was extracted from (`edu-vbos-finch :app` module):

- **MockK** for mocking, **Turbine** for `Flow`/`StateFlow` assertions, **Robolectric** for Android-dependent unit tests, **JUnit 4**.
- Patterns for ViewModel tests, Repository tests, WorkManager tests, and instrumented UI tests.
- Points at canonical in-repo examples rather than re-inventing patterns.

```
/toolbox:android-testing
```

---

### `cleanup-merged-branch`

End-of-branch hygiene. Typical triggers: *"PR was merged, clean up"*, *"we're done with this branch"*, *"switch back to main"*.

- Detects **standard merges** (via `git`) **and** **squash/rebase merges** (via `gh pr` when available).
- Aborts cleanly if the branch is **not** actually merged — leaves the worktree and branch untouched. No destructive surprises.

```
/toolbox:cleanup-merged-branch
```

---

### Updating

After editing skill files locally:

```
/plugin marketplace update
```

---

## 中文版

Andy Lai 的 Claude Code plugin marketplace。重點是 **Outside-In TDD**（又稱 **Double-Loop TDD** / **BDD + TDD**）流程、Android 測試慣例、AC 改寫、branch 收尾。

### 安裝

```bash
# 本機路徑
/plugin marketplace add ~/andy-marketplace

# 或之後 push 到 GitHub 後
/plugin marketplace add https://github.com/AndyLai13/andy-marketplace

# 裝 toolbox plugin
/plugin install toolbox@andy-marketplace
```

裝完所有 skill 都在 `toolbox:` namespace 下。

| Skill | 用途 | 叫用 |
|---|---|---|
| [`dual-loop-flow`](#dual-loop-flow--雙循環-tdd--outside-in-tdd-規劃器) | 多 PR feature 縱切 + **Outside-In TDD** 規劃（Foundation-freeze 模型） | `/toolbox:dual-loop-flow` |
| [`gwt`](#gwt--given-when-then-改寫) | 把 AC 改寫/檢查成 **BDD 風格** Given-When-Then | `/toolbox:gwt` |
| [`test-funnel`](#test-funnel--ac--測試漏斗unit--instrumented--manual) | 把 AC 轉成 TC 表格文件，逐列分流成 **unit / instrumented / manual** —— 再把測試碼寫出來 | `/toolbox:test-funnel` |
| [`android-testing`](#android-testing-1) | Android `:app` 測試慣例（MockK / Turbine / Robolectric / JUnit 4） | `/toolbox:android-testing` |
| [`cleanup-merged-branch`](#cleanup-merged-branch-1) | feature branch 收尾：同步 main、確認已 merge（含 squash） 才刪 local | `/toolbox:cleanup-merged-branch` |
| `start-task` | 開 PR 前收斂 pipeline：worktree → `/review-pr` 迴圈 → spotless → 整理 commit | `/toolbox:start-task` |
| `roborazzi-figma` | Figma↔Android 像素比對迴圈：render → diff → fix（Roborazzi），收斂後鎖成 CI golden | `/toolbox:roborazzi-figma` |

`dual-loop-flow` 在 AC 前置步驟會引用 `/toolbox:gwt` —— 兩者同包，裝 `toolbox` 即同時具備。

---

### `dual-loop-flow` — 雙循環 TDD / Outside-In TDD 規劃器

核心方法論 skill。把一個多 PR feature **縱切（依 AC group）** 並用 **Double-Loop TDD**（亦即 **Outside-In TDD**、**BDD + TDD** 雙循環）來規劃：

- **外圈 (BDD / acceptance)**：先寫一條會失敗的 acceptance test 把整個 slice 框起來。
- **內圈 (TDD / unit)**：在外圈紅燈內，用傳統 red/green/refactor 一輪一輪推進實作，直到外圈轉綠。

> "Outside-In" 的精神是 **從最外層的使用者行為斷言（AC → acceptance test）開始，往內推單元測試**。外圈在整個 slice 完成前持續紅燈；內圈在外圈紅燈內反覆 red→green→refactor —— 這就是 **雙循環**。

**Skill 會產出什麼**

對 N 個 AC group 的 feature，會規劃：

1. **PR #1 — Foundation PR（β 風格）**：把所有 AC 對應的 **Given/When/Then 斷言整段寫完**，全部標 `@Ignore("WIP: ...")`。Day 1 就把契約鎖住。
2. **PR #2…N — Slice PR（一個 AC group 一個）**：每個 PR 只 un-ignore 自己那條外圈測試，再用內圈 TDD（Domain → ViewModel → UI）把它推到綠燈，獨立合進 main。
3. **Final PR**：收尾、處理 design TBD、開 flag。

因為外圈測試在 **Foundation 就鎖死**，slice PR 可以**任意順序合進 main**，且 main 一路保持綠燈 —— 這就是 Foundation-freeze 的保證。

**為什麼規則嚴格（不能 override）**

Skill 會拒絕：橫切、α 風格骨架外圈測試、從 PRD 文字推 IA 決策。每條規則都是結構性的 —— 一旦破壞，Double-Loop / Foundation-freeze 模型直接崩掉（理由很長，見 skill 內文）。

**Phase 8 手動測試 gate**：每個 slice PR 完成前要把該 slice 的手動測試案例 finalize 到 `test_case/<slice>.md`，補上 UI/E2E 自動化還沒到位的空檔。

**Integration mode**：OPSX / Figma / Jira 有就用、沒有就降級，**永遠不會卡死**。

**叫用**

```
/toolbox:dual-loop-flow <feature-name>
/toolbox:dual-loop-flow <parent-jira-ticket>
```

**適用時機**：feature 拆得出 5+ PR、AC 已 freeze（或接近 freeze）、trunk-based + feature flag。
**不適用時機**：單 PR 小改、AC 還沒寫好（先跑 `/toolbox:gwt`）、1–2 PR 就做完。

---

### `gwt` — Given-When-Then 改寫

把 AC 改寫/檢查成具體的 **BDD 風格 Given/When/Then** —— 這是 **Outside-In TDD** 外圈 acceptance test 的前置形式。

輸出到本機 `test_case/*.md`（不寫回 Jira）。和 `dual-loop-flow` 直接搭配：先用 `gwt` 把 AC 收緊，再用 `dual-loop-flow` 規劃 PR。

```
/toolbox:gwt
```

---

### `test-funnel` — AC → 測試漏斗（unit / instrumented / manual）

把 feature 的 AC/PRD 轉成 **TC 表格文件**，並對每個 sub-case 走一條 **測試漏斗**：預設一切都是「手動驗」，再往下游拉 —— 純邏輯下沉成 **unit**（回歸防線）、原本要人工跑但能自動化的升成 **instrumented**（真機測試）、只剩純視覺/感知的留 **manual**。然後**真的把測試碼寫出來** —— unit 的命名/stack 委派 `android-testing`，instrumented 用內附 harness（`ActivityScenario` 拉非-exported Activity、手勢注入、settle-gating 避 flake）。

`✅`（instrumented）的界線是**看路徑，不是看值**：instrumented 只保留給 unit/Robolectric 結構上搆不到的層 —— 真動畫、真手勢、真 IPC、真 lifecycle/looper-async —— 即使該值另有純函式 unit 也覆蓋。

AC 還沒整理成 GWT 時會條件式呼叫 `/toolbox:gwt`，已整理過則直接吃既有 `test_case/*.md`。TC 表格格式與 instrumented harness 萃取自 canonical 的 `edu-vbos-finch` Freezer 功能（VB-579 / VB-882）。

```
/toolbox:test-funnel <jira-key>
/toolbox:test-funnel "<貼上 AC>"
```

---

### `android-testing`

把這 skill 抽出來那個專案（`edu-vbos-finch :app` module）的測試慣例固化下來：

- **MockK** mock、**Turbine** 斷言 `Flow`/`StateFlow`、**Robolectric** 處理 Android 依賴的 unit test、**JUnit 4**。
- ViewModel、Repository、WorkManager、instrumented UI test 的模板。
- 直接指向 repo 內既有的範例檔，不重造輪子。

```
/toolbox:android-testing
```

---

### `cleanup-merged-branch`

Feature branch 收尾。典型 trigger：「PR merge 了，清一下」、「這 branch 做完了」、「回 main」。

- 同時辨識 **標準 merge**（用 `git`）和 **squash / rebase merge**（有 `gh` 時用 `gh pr`）。
- **沒真的 merge 就 abort**，worktree 和 branch 完全不動，不會誤刪。

```
/toolbox:cleanup-merged-branch
```

---

### 更新

修改本機 skill 檔案後：

```
/plugin marketplace update
```
