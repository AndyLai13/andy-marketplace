---
name: dual-loop-flow
description: 以縱切（vertical slice）+ 雙循環 TDD（double-loop TDD，亦稱 outside-in TDD）工作流規劃多 PR feature。適用情境：feature 需要 5+ PR、AC 已凍結、trunk-based 環境。OPSX / Figma / Jira 有就用、沒有則優雅降級（不卡住）。Trigger：`/toolbox:dual-loop-flow <feature-name>` 或 `/toolbox:dual-loop-flow <parent-ticket>`。
---

# dual-loop-flow — 縱切 + 雙循環 TDD 工作流

以縱切（依 AC 分組）+ 雙循環 TDD（亦稱 **outside-in TDD**：outer acceptance test 由外往內驅動 inner unit test）規劃並結構化多 PR feature。產物在 OpenSpec（`tasks.md`）與 Jira sub-task **當這些整合存在時**落地；當它們（或 Figma）缺席時，skill 優雅降級而非停擺 — 見 §整合模式。

## 何時使用

✅ User 想規劃多 PR feature（5+ PR）
✅ AC 已寫進 Jira 且 PM 確認過（已凍結或接近凍結）
✅ Trunk-based 開發環境 + feature flag 機制
✅ User 透過 `/toolbox:dual-loop-flow` 觸發，或說「拆 multi-story feature」/「規劃 multi-PR」等

## 何時不要使用

❌ 單 PR 就能搞定的小修改
❌ AC 還沒定案（請先跑 `/toolbox:gwt`）
❌ feature 1-2 個 PR 就能做完
❌ 需要橫切（horizontal slicing）或 skeleton 風 PR #1 — 那種跟本 skill 的 Foundation-freeze 模型不相容（見下方）

---

## 強制工作流規則（本 skill 不可協商）

本 skill 強制執行**四條結構性規則**。它們**不是**個人偏好，而是 dual-loop TDD + Foundation-freeze 方法論的必要條件。若你的團隊需要不同規則，請別用這個 skill；改用沒有 Foundation-freeze 的純 TDD。

### 規則 1：只能縱切（依 AC 分組，**不**依技術層）

❌ **橫切**（Domain PR → ViewModel PR → UI PR → integration PR）會造成強相依鏈。PR #N+1 必須等 PR #N merge。PR 無法並行。

✅ **縱切**（每個 PR = 1 個 AC group，自帶 outer + production + inner test）讓 slice PR 可以任意順序 merge。Foundation PR 凍結 contract；slice PR 各自針對鎖定的 contract 工作，互不干擾。

**為何是結構性規則**：要保證「main 永遠綠 + slice PR 可亂序 merge」必須縱切。否則 @Ignore 保護的 Foundation 模型會崩 — 每個 slice 都會去動同樣的 Domain / ViewModel / UI class，造成衝突，被迫序列化 merge。

**Skill 行為**：Phase 3 若 user 提出橫切分組，skill 會拒絕、重新說明、再次要求縱切方案。沒有 override。

### 規則 2：β 風格 PR #1 outer test（完整 assertion + @Ignore）

❌ **α**（skeleton `@Test fun acN() { TODO() }`）讓 slice PR 各自寫 assertion code。這破壞 Foundation-freeze：沒有鎖定的 contract；slice PR 對「AC 真正的意思」會漂移。

❌ **γ**（簡單 AC 用 β、複雜 AC 用 α 混合）導致 contract 覆蓋不一致；「Foundation 鎖住所有 AC 行為」的一致性破功。

✅ **β**（PR #1 寫完整 Given/When/Then assertion code，全部 `@Ignore("WIP: ...")`）在 Day 1 鎖定 contract。Slice PR un-ignore + 實作直到 GREEN，對著被鎖定的 test 寫。Slice 作者若認為某個 assertion 有問題，要另開 PR #1.x 修 test — **不要**在自己的 slice PR 改。

**為何是結構性規則**：@Ignore + Foundation 機制要求 outer test body 在切片開始前**完整且鎖定**。α / γ 會打掉這點 — Foundation 不再凍結任何可執行的東西。

**Skill 行為**：Phase 4 skill 確認 β（不會 AskUserQuestion 給選項）並解釋規則。沒有 override。

### 規則 3：IA / 入口形態決議必須 Figma 確認（**不**得從 PRD 文字 finalize）

❌ **從 PRD 字面決議入口容器 / 位置 / form factor**。PRD 文字對 IA 是結構性歧義的：「Side Tool Bar 上的 Timer 入口」可以是 STB 上獨立按鈕、貼著 STB 邊緣的 inline chip row、從 STB 拉出的 dropdown … 從 PRD 文字鎖死 IA 等於賭整個 Foundation contract。

✅ **IA 決議只能有 Figma 確認，否則標 `[Figma TBD]` + Final PR gate**。Style 層級的 TBD（顏色、drop-shadow、scrim opacity）可以 placeholder 後補 skin；IA 層級的決議（入口住在哪、什麼容器裝它、什麼 form factor）**必須**在 Foundation 凍結前 Figma 確認，否則就明示 defer 到 Final PR 並開好 TBD task。

**為何是結構性規則**：Foundation 鎖 outer-test contract。若 outer test 斷言錯誤容器的狀態（例如 `FinchSideButtonController` slot 2，但 Figma 顯示 widget panel inline chip），則：
- 所有 slice PR 在錯的入口模型上寫 production code
- Slice test un-ignore 然後對著錯的 assertion 變綠 → 假信心
- 晚發現 → 不是 Final PR 補個 skin，是整個 slice plan 重組
- 代價對稱於規則 1：IA 鎖錯 Foundation-freeze 跟橫切一樣會崩 — 而且更糟，因為 outer test 主動編碼了錯的 contract

分類 gate：**這個決議是否改變使用者點什麼元素 / 那個元素住在哪 / 什麼容器裝它？** 是 → IA 層級 → 必須 Figma。否（純視覺 skin）→ style 層級 → placeholder 可。

**Skill 行為**：Phase 2 在生成或檢視 design.md 時，掃所有 Decision (D1, D2, …) 的 IA 內容。對每個 IA Decision：
- 已 finalize 但沒 Figma citation → 拒絕；要求 (a) Decision 本文加 Figma reference / screenshot citation，或 (b) 改寫成 `[Figma TBD]` 並把對應 TBD task 加進 Final PR
- Figma 尚未提供 → 強制 `[Figma TBD]` 形式，**不要**從 PRD 字面 finalize

Phase 0 Q4（Figma 凍結狀態）餵這個 gate：若狀態是「Partial TBD」或「Not started」→ Phase 2 必須把 IA Decision 浮上來標 Figma-blocked，即使 user 想從 PRD 文字 finalize。

### 規則 3b：Element Inventory Gate（規則 3 的後續 — style/skin 不再豁免 Figma）

> 對每個被規則 3 IA 層級 Decision 點到的 UI surface，在實作它的 slice PR merge 前，design.md **必須**包含一張 **Element Inventory 表**：每個 Figma child node（icon、badge、decoration、arc、indicator、text label）一列，每列 cite `nodeId` 並把 `{geometry | color | size}` 標為 *Figma-locked*、*Token-mapped*、或 *Provisional → Final PR gate task created*。任何標 `Provisional` 的元素**必須**在 Final PR section 開一條 checkbox task（不是只留 comment「Design QA tracks separately」）。實作者自行 defer 的 QA 不滿足這個 gate。

**為何是結構性規則**：規則 3 只把「Figma 對齊」窄化到 macro IA（入口位置 / 容器 / form factor），而把 child-node 視覺（chip icon、arc geometry、decoration color）歸到另外的「Styling」/「Implementation」類別、豁免 Figma citation。VB-82 Dashboard chip widget 就因此出貨時 5/10/15 chip 漏了 play button、arc 的 geometry/color/結構也錯 — 從未對著 Figma 逐 pixel 比對，因為：
- spec.md 只抽象要求「四個 chip：5 / 10 / 15 / +（icon）」
- tasks.md 只叫 dev 畫個 arc，沒有「match Figma」的 verify step
- 唯一碰到 chip 視覺的 D-entry 把 scope 限縮在 arc 的語意 invariant（「純裝飾、不得 animate」），沒管它的 geometry/color

→ Macro IA 正確是必要但不充分。每個被 IA 層級 Decision 點到的 UI surface，其 child-node 視覺都要明確分類成三態之一，`Provisional` 強制開一條 Final PR checkbox。

**Element Inventory 表範例**（VB-82 chip widget — 反面教材的正解）：

| Element | Figma nodeId | Geometry | Color | Size | Status |
|---|---|---|---|---|---|
| `chip_arc` | `8011:23047` | arc start/sweep angle, stroke width | `vsds_sys_color_primary_60` | container 64×64dp | **Figma-locked** |
| `chip_play_button` | `8011:23052` | 12×12dp triangle centered | `vsds_sys_color_on_primary` | 12dp | **Figma-locked** |
| `chip_text` | `8011:23049` | baseline 8dp below center | TextAppearance.VSDS.LabelLarge | — | **Token-mapped** |
| `plus_chip` | `8011:23070` | icon 16dp centered, no arc | `vsds_sys_color_on_surface` | container 64×64dp | **Figma-locked** |

若 `chip_play_button` 被標 `Provisional`，Final PR `tasks.md` **必須**有 `- [ ] Bind chip_play_button geometry to Figma node 8011:23052 (currently provisional)` — 不是只留一條 code comment。

**反模式（不接受）**：實作者留 comment 像

> // Geometry is provisional pending the Figma asset Design QA tracks separately

或

> // TODO: Design QA will adjust color in a later PR

這些**不是**合法的 deferral。「provisional」「pending Design QA」「tracks separately」「Design QA will adjust」這些字眼都代表 Final PR `tasks.md` 裡**必須**有一條 checkbox task，帶該元素的 `nodeId` 與具體 deliverable。實作者自行 defer 的 QA 不滿足這個 gate。

**Skill 行為**（Phase 2 後續）：對 Phase 2 確認的每個 IA 層級 surface，立刻對該 node 呼叫 `mcp__claude_ai_Figma__get_metadata`，枚舉每個 child node，把 Element Inventory 表草稿 emit 進 `design.md`。若 Figma MCP 不可用，**降級而非 block**（見 §整合模式）：Element Inventory 列改由使用者手動提供，或整面標 `Provisional` + Final-PR gate task。永不降級的底線：讓某 surface *完全沒有* child-node 分類就出貨、或從 PRD 文字 finalize IA — 不論 Figma 是否可用都禁止。

### 規則 4：UI 是兩軌 — 互動軌進 slice 測試、視覺軌進 slice 驗收（不 TDD、不留到 Final PR 補）

❌ **把「UI」當成一個不分軌的整體、等雙循環跑完才回頭補。** Skill 的 outer test 刻意在 ViewModel 抽象層斷言行為（規則 3：`viewModel.onPresetSelected(...)`，不是 `R.id.timer_stb_button`）。這讓 outer ring 對 IA 變動穩健 — 但也代表實際的 View↔ViewModel wiring 與螢幕上的視覺，在循環內**沒有任何 test 在驅動**。結果：Domain→VM 被鑿穿、循環變綠，真正的 UI 被推到「之後」→ 變成獨立的 retrofit pass。

✅ **把 UI 拆兩軌，兩軌都在擁有該 surface 的 slice 內交付：**

| UI 的一半 | 可測？ | 本 skill 的軌道 |
|---|---|---|
| **互動 / 行為**（點真實元素 → VM 呼叫 / state 變化） | ✅ 可 | 一條 **View-binding 整合測試**（Robolectric/Espresso）— outer-acceptance(VM) 與 inner-unit 之間缺的中間 ring。住在 slice 內。 |
| **視覺 / 皮**（geometry、color、padding、animation、layout） | ❌ 不可 — 斷言它 = 把 layout 抄一份進 test | **視覺驗收**：對 Figma + 規則 3b Element Inventory。**不是** assertion test。在 slice 內用 screenshot / 手動比對完成，不整包 defer 到 Final PR。 |

**為何是結構性規則**：雙循環 TDD 中 outer 與 inner ring 由一層**整合測試**橋接 — 對 Web endpoint 是 HTTP 層測試；對 Android UI 就是 View↔VM binding 測試 — 而**視覺 UI 不該被 TDD**（斷言 padding/color/layout = 把 layout 抄進 test，零保護價值）。漏掉 A 軌 → UI wiring 沒被測；漏掉 B 軌的 slice-scoping → 視覺滑到 Final PR 補丁桶。任一漏掉都會重現「做完才回頭補 UI」。

**Skill 行為**：
- Phase 2 的 Element Inventory 枚舉涵蓋**每個 slice 會動到的 UI surface** — 不只 IA-Decision surface（見 Phase 2）。
- Phase 6 slice-task 模板帶 UI deliverable：一條 View-binding 整合測試 + 該 surface 的視覺實作，由「視覺 frame 到位」的 slice-entry 前置條件 gate 住。Slice DoD 不能只靠 VM-level outer green 滿足。
- Final PR 只收 **genuine Figma-TBD**（slice 當下 Figma 真的還沒出的素材），絕不收 UI 工作的主體。

### Asset provision policy（何時交付 Figma / 截圖）

UI 素材分**階段、分層**提供 — 不是一次全給，也不是拖到最後才給。兩層、兩時點、兩種失敗模式：

| 素材 | 鎖住什麼 | 最晚可接受時點 | 放哪裡 |
|---|---|---|---|
| **IA 層 Figma**（入口位置 / 容器 / form factor） | Foundation outer-test contract | **Phase 2**，Foundation 凍結前（規則 3） | design.md Decision body citation（link / frame X confirmed YYYY-MM-DD） |
| **Element Inventory child node**（icon/arc/badge/text nodeId） | 規則 3b 分類表 | **Phase 2**，用 `get_metadata` | design.md Element Inventory 表（`nodeId` 欄） |
| **單 slice 的視覺 frame + 截圖**（該 surface pixel 細節） | 該 slice 的視覺保真 | **該 slice PR 開工前**（just-in-time；kickoff-complete 專案在 Phase 2 batch） | slice-entry 前置條件 + repo `assets/` 路徑，design.md 引用 |
| **Style 微調**（color / shadow / scrim） | 只皮 | 可 defer 到 **Final PR**（先 placeholder token） | Final PR genuine-TBD checkbox |

**placeholder-on-unavailable 規則（僅限 style 層）：**
- **IA 層缺 → BLOCK，絕不 placeholder。** IA 錯 = contract 錯 = 整個 slice plan 重組（規則 3）。kickoff-complete 專案在 Phase 2 後不該再發生；若發生 = Phase 2 沒做完 → 停下回去重做 Phase 2，不要從 PRD 猜。
- **Style 層在 dev 期缺 + 作者聯絡不到 → 只有在 Phase 0 Q5 取得 standing 預先授權時才可 placeholder。** 一般互動 session 的 `AskUserQuestion` 是阻塞的（沒有真的「N 分鐘自動 timeout」）；「等 ~10 分鐘再 placeholder」只在 user 已用 Phase 0 Q5 預先授權、或在 autonomous `/loop` 裡才成立。授權成立時：放 VSDS-token / provisional placeholder、把 Element Inventory 該列標 `Provisional`、開一條帶 `nodeId` 的 Final-PR backfill checkbox。Style 層絕不卡 user。
- **每個 placeholder 都必須可追蹤**（Element Inventory `Provisional` 列 + Final-PR `- [ ]` checkbox 帶 `nodeId`）。光留 code comment（`// provisional pending Figma`）**不是**合法 deferral（規則 3b）— VB-82 chip 漏 play button 正是這個漏洞造成的。

### Playbook 為什麼還寫 α / γ？

`~/.claude/docs/feature-pr-slicing-playbook.md` §4 為了完整性記錄 α / γ — 它們在其他 context 是有效的 TDD 變體（AC 還在飄的多人團隊、每位 dev 自己寫 test 的學習情境）。**但它們跟本 skill 的 Foundation-freeze 模型不相容**，所以本 skill 只強制執行 β。

---

## 整合模式（OPSX / Figma / Jira — 先偵測，再優雅降級）

本 skill 用三個外部整合。**Phase 0 偵測每一個；有就用，沒有就降級 — 絕不因為缺工具而硬停。** dual-loop + 縱切方法論（上方四條規則）完全與工具無關；OPSX / Jira / Figma 是交付載具，不是方法本體。

**兩種「缺」— 不要混為一談：**
- **缺工具**（沒 `openspec` CLI / Jira 沒接 / Figma MCP 未認證）→ 降級繼續跑。✅ 這正是 mode 的目的。
- **缺資訊**（IA 還沒在 Figma 凍結）→ 這是規則 3 的 gate，**不是**工具問題。降級成 `[Figma TBD]` + Final-PR gate task；計畫仍然走完。❌ **絕不把這降級成「從 PRD 文字 finalize IA」** — 那正是規則 3 要防的 VB-82 失敗模式。Figma 越少 = `TBD` 越多，絕不是猜越多。

| 整合 | 偵測（Phase 0） | 有 → | 無 → 降級成 |
|---|---|---|---|
| **OPSX**（OpenSpec） | `openspec --version` 可跑 / repo 有 `openspec/` | scaffold + `openspec validate` + propose→apply→archive 生命週期 | 產物寫到 `docs/plans/<date>-<feature>/`；跳過所有 `openspec new/validate/archive`；省略 `/opsx:archive` 提醒 |
| **Jira** | Atlassian MCP 可達 **且** 有給 parent ticket | Phase 1 `getJiraIssue`；Phase 7 建 N+2 個 sub-task + backfill key | AC 改由使用者貼上 / 本地檔；Phase 7 在 `tasks.md` 內出**sub-task 計畫表**（不建 ticket）；backfill 欄 = `Jira: N/A` |
| **Figma** | Figma MCP 已認證 **且** 有 frame | IA 確認 + `get_metadata` 自動 Element Inventory | IA Decision → 全標 `[Figma TBD]` + Final-PR gate；Element Inventory → 使用者手動提供列 或整面標 `Provisional` + Final-PR gate。**仍絕不從 PRD finalize IA（規則 3）。** |

**讓降級被看見，絕不藏：**
- **Phase 0** 在發問前印一行能力矩陣（例 `OPSX=on · Jira=off · Figma=TBD`），讓使用者一開始就知道哪些路徑被降級。
- **Phase 8** 輸出印一份**降級 manifest** — 每個被降級的步驟、每個轉成 `TBD` 的項目 — 讓「因缺工具而沒做」永遠不會被誤讀成「做了」。

---

## 參考資料（按需取用）

**全域（skill 內建依賴）**：
- **Playbook（理論）**：`~/.claude/docs/feature-pr-slicing-playbook.md` — 完整方法論、決策樹、失敗模式
- **SOP（checklist）**：`~/.claude/docs/feature-sop-checklist.md` — Phase 1-7 可執行步驟

**專案層（專案 docs，跨環境可攜）**：
- **Testing conventions**：`<project-root>/docs/testing-conventions.md` — TestCase 三層分流、manual 元件清單、TestCase folder 位置
- **Project CLAUDE.md**：`<project-root>/CLAUDE.md` — 該專案的 testing pipeline 摘要 + 連結到 testing-conventions.md

**Personal memory cross-refs（選用，純歷史脈絡）**：
- `[[feedback-pr-vertical-slicing]]`、`[[feedback-outer-test-full-assertion]]` — 規則制定原因記錄（規則本身已內建於本 skill §強制工作流規則）
- `[[opsx-with-double-loop-tdd]]` — 歷史脈絡（被本 skill supersede）

> **Skill 跨環境 portability**：global docs + project docs 即可運作；β + 縱切兩條核心規則已強制內建（不靠 memory）。memory 只是個人 rationale 紀錄。

## Workflow

帶 user 走 Phase 0-8。決策點用 `AskUserQuestion`；檔案改動用 `Edit` / `Write`；建立 sub-task 用 Jira MCP tool。

### Phase 0：Scope intake

**首先**：讀專案的 `docs/testing-conventions.md`（若存在）以了解 TestCase folder 位置、manual 元件清單、該 codebase 的 pipeline 慣例。若不存在，skill 仍能運作但 `[manual]` artifact 檢查需要 user 手動輸入。

**接著偵測整合**（見 §整合模式）：探測 `openspec --version` / `openspec/` 目錄（OPSX）、Atlassian MCP 可達性 + 是否有給 parent ticket（Jira）、Figma MCP 認證（Figma）。記錄 `OPSX / Jira / Figma` 能力旗標，並在發問前**把能力矩陣印給使用者**（例 `OPSX=on · Jira=off · Figma=TBD`）。每個缺席的整合把對應 phase 切到降級路徑 — 絕不因為缺一個而停。

用 `AskUserQuestion`（5 問題；缺席整合讓某題失去意義時可跳過 / 調整）：

1. **Parent ticket key**（例如「VB-200」）— *Jira 模式*。Jira 缺席則跳過；改用 feature name 當 plan id，AC 在 Phase 1 收。
2. **Feature name**（kebab-case，例如 `add-foo`）
3. **Multi-story 狀態**：
   - Standalone（單一 Story）
   - Multi-story：第幾支 + 依賴哪些前置 Story Foundation PR
4. **Figma 凍結狀態** — 分兩層問，因為 IA 與 styling 的 TBD 容忍度不同（見規則 3）：
   - **IA 層**（入口位置 / 容器形態 / form factor）：完全凍結 / 部分 TBD（列出哪些 AC 涉及）/ 還沒開始
   - **Styling 層**（顏色 / 字級 / drop-shadow / icon 細節）：完全凍結 / 部分 TBD / 還沒開始

   ⚠️ 若 **IA 層為「部分 TBD」或「還沒開始」** → Phase 2 design.md 必須把對應 Decision 標 `[Figma TBD]`，**不**得從 PRD 文字 finalize（規則 3）。Styling 層 TBD 可接受 placeholder + Final PR 補完。
5. **Style 素材 standing 預先授權** — 一條 standing rule，governs 所有後續 slice（餵 Asset provision policy）：
   - 「開發中若缺 **style 層** 素材且你當下不在 → 預先授權放 placeholder + 開 Final PR backfill checkbox（不卡你）」：Yes / No
   - ⚠️ 此授權**僅限 style 層**。**IA 層一律 block 等你確認**（規則 3 / 規則 4 Asset provision policy），不在此授權範圍。
   - 記錄答案；Phase 6 slice-entry gate 與 dev 期 placeholder 行為都依此判斷。

若 AC 還沒過 `/toolbox:gwt`：請 user 先跑 `/toolbox:gwt`，不要繼續。

### Phase 1：AC integrity check

取得 AC，依模式：
- **Jira 有**：`getJiraIssue`（markdown format，fields=`["summary", "customfield_12203"]`）。
- **Jira 無**：請使用者貼上 AC 或指向本地 AC 檔（例如 `/toolbox:gwt` 的產出）；讀那個。把來源路徑記進 plan 供追蹤。

計算 outer test 數量：
- AC 數：N
- Outer test：Σ scenarios（多 scenario invariant AC 要算進去）

繼續前先給 user 看分解表。

### Phase 2：change scaffold（OPSX 有）/ plan-doc scaffold（OPSX 無）

**OPSX 有** — 若 `openspec/changes/*-<feature>` 不存在：
- 跑 `openspec new change "<feature>"`
- 用日期前綴 rename：`mv openspec/changes/<feature> openspec/changes/YYYY-MM-DD-<feature>`（用 `date +%Y-%m-%d`）
- 產出 `proposal.md` / `design.md` / `specs/<capability>/spec.md`（若 user 還沒跑 /opsx:propose）
- 若已存在，讀現有 artifact 了解 context。

**OPSX 無** — 建立 `docs/plans/YYYY-MM-DD-<feature>/`，放 `design.md`（之後放 `tasks.md`）；跳過所有 `openspec` 指令（不 `new`、不 `validate`）。沒有 `proposal.md` / `spec.md` delta 追蹤 — 在 manifest 註明 OpenSpec 的 spec-lint + 生命週期不可用。

本 phase 以下（IA gate、Element Inventory）**兩種模式完全相同** — 不論 `design.md` 放哪，都對它操作。

**IA gate（規則 3 強制）** — 驗證前先掃 `design.md` 的 Decisions 找 IA 層級內容：

- 對每個 `### D<N>:` Decision，分類其主題：
  - **IA 層級**：Decision 改變了*入口住在哪、什麼容器裝它、什麼 form factor*（例：「Timer 入口 — STB slot 2 vs widget panel inline chip row」、「Floating clock close 鍵 — 右上角 vs floating handle」）
  - **Style 層級**：純視覺 skin（顏色、drop-shadow、scrim opacity、icon polish）
- 對每個 IA 層級 Decision：
  - **必須**在 Decision 本文 cite Figma（screenshot path / Figma link / 「Figma frame X confirmed YYYY-MM-DD」），或
  - **必須**標 `**[Figma TBD]**`，且對應的 TBD task 已寫進 `tasks.md` Final PR section
- 若 Decision 從 PRD 文字 finalize（例如「**為什麼**：PRD 寫明 …」但沒 Figma citation）→ flag 給 user、拒絕繼續，直到 (a) 加上 Figma 確認，或 (b) Decision 改寫成 `[Figma TBD]`

這條防的坑：PRD 文字對 IA 結構性歧義。「Side Tool Bar 上的 Timer 入口」可以是 STB 上獨立按鈕、貼著 STB 的 inline chip row、或 dropdown — Figma 是入口位置的唯一 source of truth。

**Element Inventory gate（規則 3b + 規則 4 強制）** — 對**每個 slice 會動到的 UI surface**枚舉 Element Inventory，不只 IA-Decision surface：

- 範圍內 surface = (a) 通過規則 3 gate 的每個 IA 層級 surface，**加上** (b) 任何計畫中的 slice 會 render 或 mutate 的其他 UI surface（chip、panel、dialog、overlay、list row…）。規則 4：沒被任何 IA Decision 提到的視覺，仍需 child-node 分類，否則它會默默滑到 Final-PR retrofit。
- 對每個 surface，對其 Figma node 呼叫 `mcp__claude_ai_Figma__get_metadata` 枚舉每個 child node（icon、badge、decoration、arc、indicator、text label）。
- 每個 child 在 `design.md` Element Inventory 表 emit 一列：`Element | nodeId | Geometry | Color | Size | Status`。Status **必須**是：**Figma-locked**、**Token-mapped**、或 **Provisional** 三者之一。
- 每個 `Provisional` 列 → 在 `tasks.md` Final PR section 草稿一條 `- [ ] ...` checkbox（Phase 6 會接手）。code comment 像「Design QA tracks separately」「provisional pending Figma asset」**不是**替代 — 拒絕並強制開 checkbox task。
- **若 Figma MCP 不可用（未認證 / 沒 frame）→ 降級，不要 block**（見 §整合模式）：請使用者手動提供 Element Inventory 列，或整面標 `Provisional` + Final-PR gate task，並把該 surface 的每個 IA 層級 Decision 轉成 `[Figma TBD]`。計畫帶著那些視覺被明確標記後走完。❗ 唯一不降級的：不得為了「補洞」從 PRD 文字 finalize IA（規則 3）。

**Asset batch（kickoff-complete 專案）** — 當 Phase 0 Q4 回報 Figma 在 kickoff 大致凍結時：在這裡一次 batch 拉齊所有確認素材（IA Figma + 每個 surface 的 child node + 截圖進 repo `assets/`），讓每個 slice 的視覺 frame 已在手上。Phase 6 的 per-slice slice-entry gate 就退化成便宜的前置檢查、而非 stop-and-wait。真的還沒出的視覺 → `Provisional` + Final-PR checkbox（genuine TBD）。

驗證（**僅 OPSX 有**）：`openspec validate "<change-name>"`。OPSX 無 → 跳過；改靠上面的人工 IA / Element-Inventory gate（manifest 註明少了 spec-lint）。

### Phase 3：把 AC 縱切成 group（強制規則 1）

⚠️ **只能縱切** — 見強制工作流規則 §規則 1。

提出切片方案，再用 `AskUserQuestion` 確認：

- 顯示 AC list 並依**功能凝聚度**自動建議分組
- 每個 slice：5-10 個 outer test 為理想；> 15 警告 + 建議再切
- 若有 Figma TBD：標出哪些 slice 涉及，placeholder layout + Final 補完

**若 user 提出橫切**（依 Domain / ViewModel / UI 層）：拒絕，重新說明規則 1，重新要求。**不要**接受橫切 — 那會破壞 Foundation-freeze。

允許 user 互動式調整**縱切**分組（slice 邊界、AC↔slice 對應）。

### Phase 4：確認 β 風格（強制規則 2 — 無選擇）

⚠️ **β 是強制的** — 見強制工作流規則 §規則 2。**不要 AskUserQuestion。**

告知 user（不是問）：

> 「PR #1 採 β 風格：完整 outer test assertion + @Ignore。Slice PR 只 un-ignore + 寫 production，不改 assertion。
> 此為 skill 強制規則（α / γ 與 Foundation-freeze 模型不相容）。」

若 user 問「為什麼不用 α / γ」，引用 playbook §4 — 解釋 α/γ 作為替代方案存在，但本 skill 只執行 β。

### Phase 5：Flag flip 策略

若單 Story：
- 單一 flag，Final PR flip

若 multi-story（Phase 0 已得知）：
- `AskUserQuestion`：Option A / B / C / D（playbook §12）
- 預設 **Option B**（單一 master flag，最後一個 Story 的 Final flip）

### Phase 6：產出 tasks.md

用 `Write` 產出 `tasks.md` — 路徑依模式：**OPSX 有** → `openspec/changes/<date>-<feature>/tasks.md`；**OPSX 無** → `docs/plans/<date>-<feature>/tasks.md`（與 Phase 2 的 `design.md` 同處）。

**Header**：
- 雙循環 TDD + 縱切策略說明
- AC ↔ slice 對應 table
- 相依性矩陣 table（含 TBD Jira key）
- 依賴圖（ASCII）

**Foundation Task（Task 1）**：
- Skeleton interface / domain / fake
- 跨 Story refactor（若 multi-story）
- 所有 outer `@Test` method，**β 風格完整 assertion** + `@Ignore("WIP: <ticket> §N, unblock at §M")`
- BuildConfig.<FLAG> = false
- Test class：`<Feature>AcceptanceTest.kt`，含 AC↔test mapping 註解

**Slice Task（Task 2..N+1）**：
- 每個 `## Task N` header 加 blockquote：`> **依賴**: ... | **阻擋**: ... | **可並行**: ... | **Jira**: TBD`
- **Slice-entry 前置條件（規則 4 / Asset provision policy）**：該 slice 的視覺 frame + 截圖到位（kickoff-complete 專案 Phase 2 已 batch；若缺 → IA 層 block、style 層依 Phase 0 Q5 standing 授權 placeholder）
- Production code + un-ignore 一部分 outer test + inner test
- **UI deliverable（規則 4）** — 任何 render/mutate UI surface 的 slice：
  - **View-binding 整合測試**（Robolectric/Espresso）：點真實 view → 斷言 VM 呼叫 / observable state（補 outer-VM 與 inner-unit 之間缺的整合層）
  - **視覺實作**對齊該 surface 的 Element Inventory（非 assertion test；screenshot / 手動比對 Figma node）
- 跨 Story 動到的部分（如有）
- **Slice DoD（規則 4）**：outer(VM) green **且** View-binding 整合測試 green **且** 該 surface Element Inventory 無 `Provisional` 殘留（除 genuine Figma-TBD）。只靠 VM-level outer green **不算** slice 完成。

**Final Task（Task N+2）**：
- Multi-story coordination check（git log 驗證前置 PR 已 merge）
- **只收 genuine Figma-TBD backfill** — slice 當下真的拿不到的素材（每條帶 `nodeId` + deliverable）。⚠️ Final PR **不是 UI catch-up bucket**；UI 主體（binding test + 視覺）已在各 slice 內交付（規則 4）。
- 依 Phase 5 策略 flip flag
- 全部 outer GREEN 驗證
- Smoke test
- **Archive 提醒（僅 OPSX 有）** — Final Task 的最後一個 checkbox：`- [ ] feature ship 後跑 /opsx:archive 歸檔此 change`（multi-story：只有 LAST story 的 Final Task 帶此項）。這是規劃尾端的固定收尾，避免使用者忘記歸檔。OPSX 無 → 省略（沒有 archive 生命週期）。

驗證（**僅 OPSX 有**）：`openspec validate "<change-name>"`。OPSX 無 → 跳過。

### Phase 7：建立 Jira sub-task + backfill

**Jira 無** → 完全跳過建 ticket。改在 `tasks.md` 內 emit 一張 **sub-task 計畫表**，每個 Task（Foundation + slice + Final）一列：`PR # | Slice 名稱 | 對應 AC | 依賴/Merge 順序 | Outer 點亮數`。backfill 欄維持 `Jira: N/A`。使用者日後可照表自行建 ticket。然後跳到 Phase 8。

**Jira 有** — 對每個 Task（Foundation + slice + Final，共 N+2 個）：

用 Jira MCP `createJiraIssue`：
- `projectKey`：從 parent ticket 推
- `issueTypeName`：「Sub-task」
- `parent`：parent ticket key
- `assignee_account_id`：Phase 0 取得
- `summary`：`<ticket> PR #N — <Slice Name> (AC...)`
- `description`（markdown），至少含：
  - 內容範圍
  - Merge 順序 / 依賴（含跨 Story 如有）
  - 對應 AC
  - Outer test 點亮數
  - 回指 `openspec/changes/<name>/tasks.md` 對應 Task
  - 跨 Story 動到（如有）
- `contentFormat: "markdown"`、`responseContentFormat: "markdown"`

把回傳的 key 收成 Task→Jira map（Task N → VB-XXX）。

**把 Jira key backfill 回 tasks.md** — 關鍵紀律：
- ⚠️ **不要**用 `replace_all` — 多個 Task header 可能撞名
- 對每個 Task 用獨立 `Edit`（每個 Task header 一次 Edit + 矩陣表一次 Edit）
- 全部換完後跑 `grep "Jira.*TBD"` 確認沒漏

### Phase 8：最終驗證 + 回報

- `openspec validate "<change-name>"` pass — **僅 OPSX 有**；無 → 跳過
- `grep "Jira.*TBD" tasks.md` 沒有任何結果 — **僅 Jira 有**；無 → 改確認 sub-task 計畫表完整
- **Manual-test gate（補手動測試檢查）** — 規劃全程驅動的自動化測試（outer / inner / View-binding 整合）涵蓋不到的 AC / surface，收尾必須回頭確認並補 **manual TestCase**。⚠️ 這些 manual TestCase 是**過渡性質**——它們代表「將來會以 **UI / E2E 方式轉成自動化測試**」的行為，**不是永久手動測試**。因此每條都要當成**未來 UI/E2E 自動化候選**來登記與追蹤。**清單優先 + 補問**：
  1. **清單比對**：以 Phase 0 讀入的 `docs/testing-conventions.md` manual 元件清單為準，掃過所有 slice 規劃觸及的 surface / AC；命中清單上元件者 → 標記該 AC/surface 需補 manual TestCase。清單缺失（無 `testing-conventions.md`）→ **降級**，跳過比對、直接進補問（見 §整合模式精神：tool/資料缺席不 hard-stop）。
  2. **補問使用者**：`AskUserQuestion` 確認清單未涵蓋的手動測試需求（硬體互動、跨裝置、感官/體感、第三方整合、目前 UI/E2E 自動化尚未到位而暫時手動的時序…）—「這個 feature 還有哪些部分需要手動測試、且未來應轉成 UI/E2E 自動化（清單未列）？」
  3. **落地，不可只在 report 提一句**（同 Rule 3b 紀律：口頭提及 ≠ 有人會做）：任一命中或使用者新增的 manual 測試 → 寫進對應 slice 的 manual TestCase 清單（依 `testing-conventions.md` 的 TestCase folder 位置），並在 Final Task 補 `- [ ] 手動測試 <AC/surface>：<deliverable>（未來轉 UI/E2E 自動化）` checkbox。每條都明確標記為 **future UI/E2E automation candidate**，讓它日後能被撈出來轉自動化、而不是被遺忘成永久手動。若確認完全不需手動測試 → 明確記「手動測試：無」，不要留空白。
- 回報給 user：
  - tasks.md 路徑（OPSX `openspec/changes/...` 或 plan-doc `docs/plans/...`）
  - **Jira 有**：建立的 N+2 個 sub-task list · **Jira 無**：指向 sub-task 計畫表
  - **降級 manifest** — 對每個缺席整合，列出降級了什麼、什麼轉成 `TBD`（例如「Figma off → 3 個 IA Decision = [Figma TBD] + Final-PR gate；OPSX off → 無 spec-lint/archive；Jira off → sub-task 在表內、未建立」）。三者都在則 manifest 為空。
  - **手動測試需求** — 列出需補的 manual TestCase（AC/surface + 對應 Final Task checkbox），每條註明為**未來 UI/E2E 自動化候選**；或「無」。若因無 `testing-conventions.md` 而只靠補問判斷，註明清單比對已降級
  - 遇到的摩擦點（若有非預期）
  - 下一步：「準備好開始 PR #1 Foundation 嗎？」
  - 收尾提醒（**僅 OPSX 有**）：feature ship（最後 Final PR merge + flag flip）後跑 `/opsx:archive` 歸檔此 change，別忘記

## 紀律 / 約束（執行時強制）

### β 風格 PR #1 outer test（強制規則 — 見強制工作流規則 §規則 2）

- PR #1 包含所有 outer `@Test`，含完整 Given/When/Then assertion code
- 全部 `@Ignore("WIP: <ticket> §N, unblock at §M")`，理由要明確
- 只 test **user 可觀察的 state**，絕不 mock 實作細節
- Slice 發現 assertion 有錯：另開 PR #1.1 修 test，**不要**改 production code 兜回去

### Acceptance test 命名

- Class：`<Feature>AcceptanceTest.kt`（例 `TimerSettingAcceptanceTest`）
- Method：`acN_briefName()` 或 `acN_subName()`（多 scenario）
- Class 頂端 AC↔test mapping 註解，方便 QA / PM 追蹤

### UI 兩軌交付（強制規則 — 見強制工作流規則 §規則 4）

- UI 拆兩半：**互動** → slice 內 View-binding 整合測試（可測）；**視覺** → slice 內對 Element Inventory 驗收（非 assertion test）。
- 兩半都在擁有該 surface 的 **slice 內**出貨。只靠 VM-level outer green 不算 slice 完成（見 Phase 6 Slice DoD）。
- Final PR 只收 genuine Figma-TBD，絕不收 UI 主體 — Final PR 不是 UI 補丁桶。
- 若 slice 作者想「UI 之後再補」→ 拒絕；binding test + 視覺屬於這個 slice。

### Asset 提供（見強制工作流規則 §Asset provision policy）

- IA 層素材缺 → **block**，絕不 placeholder；回去重做 Phase 2，不要從 PRD 猜。
- Style 層素材缺 + 作者聯絡不到 → **僅**在 Phase 0 Q5 standing 授權下可 placeholder；否則 block 並詢問。
- 每個 placeholder = Element Inventory `Provisional` 列 + Final-PR `- [ ]` checkbox 帶 `nodeId`。光留 code comment 不是合法 deferral（規則 3b）。

### Multi-story 協調

- 只有**最後一個** Story 的 Final PR flip master flag
- 中間 Story 的 Final PR 只驗 outer green
- 跨 Story refactor 放在目標 Story 的 Foundation PR（不是 slice）
- Final PR 有明確的 git log check 確認前置 Story 已 merge

### Figma TBD（兩層 — 見規則 3）

**Style 層級 TBD**（可 placeholder + Final PR 補 skin）：
- 顏色、字級、drop-shadow、scrim opacity、icon polish、animation curve
- Outer test 斷言行為 / 資料，不是 styling
- Slice 用 placeholder VSDS token
- Final PR 收到 Figma 確認後調整 layout / drawable

**IA 層級 TBD**（**不**得從 PRD 文字 finalize — 規則 3）：
- 入口位置（什麼容器裝它：STB / dashboard / widget panel / overlay）
- 入口 form factor（獨立按鈕 / inline chip row / dropdown / popup）
- 容器階層（top-level vs nested、屬於誰的 sibling / child）
- ❗ 若 Figma 尚未確認 → Decision 標 `[Figma TBD]`，Final PR 開明確 gate task；outer test 要在*正確抽象層級*斷言行為（例如 `viewModel.onPresetSelected(...)`）而**不**耦合到特定容器（例如 `R.id.timer_stb_button`）
- Final PR 才抓到 IA 錯 = slice plan 重組，不是 skin 補丁

### Slice 大小

- 每 slice 5-10 個 outer test：理想
- 10-15：若功能凝聚高可接受
- > 15：警告 user + 建議再切

### Backfill 安全

- backfill Jira key 進 tasks.md 時**絕不**用 `replace_all`
- 每個 Task 用獨立 `Edit`，明確 before/after string
- 用 `grep "Jira.*TBD"` 驗證

## 常見摩擦（識別 + 處理）

| 摩擦 | 處理方式 |
|---|---|
| User 想跳過 Foundation PR | 拒絕；Foundation 鎖 contract 讓 slice 可並行 |
| 用技術層切 slice（domain / VM / UI） | 拒絕（違反規則 1）；堅持以 AC group 凝聚 |
| User 要求 α / γ 風格 | 拒絕（違反規則 2）；解釋與 Foundation-freeze 不相容 |
| Outer test 去 mock 實作 | refactor 成 state-based assertion |
| AC 還沒凍結 | 停；請 user 先跑 /toolbox:gwt + PM 確認 |
| Figma 完全沒有 | 警告；建議至少先確認 AC 行為再切 |
| IA Decision 從 PRD 文字 finalize 但沒 Figma | 拒絕（規則 3 violation）；要求 Figma citation 或 `[Figma TBD]` + Final PR gate task |
| User 堅持「PRD 字面寫了 X 入口」 | 再解釋規則 3：PRD 文字對 IA 結構性歧義；Figma 是入口位置 / 容器 / form factor 的 SoT |
| IA surface 沒附 Element Inventory 表 | 拒絕（規則 3b violation）；對該 Figma node 呼叫 `get_metadata`，枚舉 child node，逐個分類 Figma-locked / Token-mapped / Provisional |
| 實作者留 comment「Design QA tracks separately」/「provisional pending Figma asset」 | 拒絕（規則 3b violation）；該 element 必須在 Final PR `tasks.md` 開 checkbox task 標明 `nodeId` 與 deliverable，code comment 不是合法 deferral |
| Figma MCP 未認證但要做 IA surface | **降級，不要 block**（§整合模式）：人工提供 Element Inventory 列 或整面標 `Provisional` + Final-PR gate；該 surface 的 IA Decision 全轉 `[Figma TBD]`。唯一不可降級：從 PRD 文字 finalize IA（規則 3）|
| OPSX（`openspec`）不存在 | 不要 block；產物寫 `docs/plans/<date>-<feature>/`，跳過 `openspec new/validate/archive`；manifest 註明少了 spec-lint + archive 生命週期 |
| Jira / Atlassian MCP 不可用 | 不要 block；AC 改由使用者貼上 / 本地檔；Phase 7 在 tasks.md 出 sub-task 計畫表（不建 ticket）；backfill 填 `Jira: N/A` |
| UI 被當成單一物、留到雙循環跑完才回頭補 | 拒絕（規則 4 violation）；拆兩軌：互動 → slice 內 View-binding 整合測試；視覺 → slice 內 Element Inventory 驗收。Final PR 只收 genuine TBD |
| Slice 只靠 VM-level outer green 就喊完成 | 不算完成（規則 4 Slice DoD）；要求 binding 整合測試 green + 視覺對齊 Element Inventory |
| 規劃只談自動化測試、沒檢查 manual 元件 | Phase 8 manual-test gate：比對 `testing-conventions.md` manual 元件清單 + 補問使用者；命中者落地成 Final Task `- [ ]` checkbox 並標記為未來 UI/E2E 自動化候選（口頭提及不算），確認無才記「手動測試：無」|
| manual TestCase 被當成永久手動測試 | 修正定位：它是過渡性質、未來會轉 UI/E2E 自動化；每條都標 future UI/E2E automation candidate，方便日後撈出轉自動化 |
| 開發中缺 style 素材、使用者當下不在 | 若 Phase 0 Q5 有 standing 預先授權 → placeholder + Provisional 列 + Final backfill checkbox(`nodeId`)；否則 block 等使用者 |
| 開發中缺 IA 素材 | Block；回 Phase 2 重做，絕不 placeholder（規則 3 / 規則 4 Asset provision policy） |
| 是 multi-story 但不確定自己是第幾支 | 停；回 Phase 0 第 3 題重問 |

## 輸出格式

Phase 8 後總結：

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
| 手動測試 | (需補的 manual TestCase：AC/surface + Final Task checkbox，標記未來 UI/E2E 自動化候選) 或 — 無 |
| 降級 manifest | (Figma/OPSX/Jira 缺席而降級或轉 TBD 的項目) 或 — 全部到位 |

下一步：實作 PR #1 Foundation 嗎？
（OPSX on 時收尾提醒：所有 PR merge + flag flip ship 後，記得跑 `/opsx:archive` 歸檔此 change）
```

## 相關 skill

- `/opsx:propose` — 先跑此產出 OpenSpec proposal/design/spec
- `/opsx:apply` — Phase 6 之後實作各 Task
- `/opsx:archive` — feature ship 後歸檔
- `/toolbox:gwt` — AC 寫成 Rule + violation scenarios（Phase 0 前置）
