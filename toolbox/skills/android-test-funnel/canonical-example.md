<!--
  乾淨 test-case 檔的 canonical 範例 — 由 /toolbox:android-test-funnel 參照（本地副本，不依賴外部 repo）。
  節錄自 edu-vbos-finch `test_case/timer.md`（VB-82 / 385 / 386）的真實列。
  完整維護規範、符號定義與 `_TEMPLATE` 的單一真實來源 = 產出 repo 的 `test_case/CONVENTIONS.md`；檔案骨架範本見同目錄 `testcase-template.md`。本檔不重複。

  本範例示範的「乾淨」特徵（對應 SKILL.md〈維護紀律〉）：
  - 純現狀：無「自動化盤點」彙總段、無歷史註記（「原本…已改 / 行為定案（日期）」）
  - 廢棄項直接移除：VB-82 section 的 TC-10 / TC-12 因移除而空缺——不補 placeholder、不留 (Deprecated)
  - 單一真實來源 = 每列「測試方式 / 備註」欄；整體覆蓋用 `grep '👁' <file>` 即時導出
  - 檔頭宣告架構限制（哪些層搆得到、哪些只能 manual），決定全檔哪些列可標 ✅
  - 真實檔末尾仍有 `## _TEMPLATE` 區塊（定義見 `test_case/CONVENTIONS.md`），本節錄略
-->

# 節錄：VB-82 Timer SETTING

> **⚠ 架構限制（檔頭宣告，決定哪些能 `✅`）**：Timer 的狀態化 UI 都是 FinchService host 的 WindowManager overlay、`FLAG_NOT_FOCUSABLE`——**instrumented 內的 UiAutomator（`UiDevice` + `By.res`/`By.desc`）可直接命中並驅動真實 overlay**（先前「搆不到」只對 shell `uiautomator dump` 成立，已於 2026-06-11 翻案）；Espresso 仍受 RootViewPicker 焦點限制搆不到 overlay root。canvas 自繪 / a11y-suppressed 元件（如 dial 中央鈕）仍只能 manual。**例外**：非 overlay 的一般 view（widget / SETTING card）可裝進 debug-only host Activity 用 `ActivityScenario` + Espresso 真機 by-id 驅動（見 TC-02 / TC-04）。

## TC-02 Dashboard 顯示 4 個 chip（flag=true）

- **Priority:** P0
- **Module:** TimerDashboard
- **Status:** Active

> **chip 組成：unit + instrumented**：組成 / affordance 是 layout 靜態子 view（可讀值）→ view-tree 斷言走 unit，並以 instrumented 在真機驗 isDisplayed + drawable resolve。「資產畫出來像不像 Figma」屬一次性資產正確性，標 👁 一次性。

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | `BuildConfig.TIMER_ENTRY_ENABLED = true` | 展開 Dashboard Panel | Timer widget 顯示，含 **5 / 10 / 15 / +** 四個 chip | ✅ `Inst#allFourChipsRenderAndAreDisplayedInARealWindow`（真機 isDisplayed × 4） | AC1；`unit:TimerDashboardWidgetTest#widget inflates four chip FrameLayouts` + `TimerSettingAcceptanceTest#ac1_widgetRendersFourChips` |
| 2 | 同上 | 觀察 5 / 10 / 15 chip 上方紅色 arc | 5 chip ~30°、10 chip ~60°、15 chip ~90° 弧度 | ✅ `Inst#presetChipsResolveArcAndPlayDrawablesOnDevice`（arc 圖層真機 resolve）/ 👁 弧度形狀對 Figma（一次性資產） | design.md D10；arc 圖層存在已 `unit:TimerDashboardWidgetTest#preset chips each show an arc decoration and a play affordance` |

<!-- 示範點：同一列可「✅ 真機 resolve」與「👁 視覺對 Figma（一次性）」並存——instrumented 驗存在/解析，視覺正確性留人眼。 -->

## TC-03 5/10/15 chip 直接 emit Countdown（不進 SETTING）

- **Priority:** P0
- **Module:** TimerDashboard
- **Status:** Active

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | flag=true、Timer 處於 IDLE | 點 Dashboard widget 的 5 chip | 不進 SETTING card；直接進倒數 ready / 暫停態完整 UI | 👁 Manual（真機點擊路由 + 真機 overlay；路由→emit 已 unit 覆蓋） | AC2；`unit:TimerDashboardWidgetTest#tap 5 chip from IDLE routes to onPresetChipFromIdle and emits Countdown` + `TimerSettingViewModelTest#onPresetChipFromIdle emits Countdown with preset minutes and recalled sound` |

<!-- 示範點：👁 必附理由（括號內），且備註欄列 unit backstop——manual 不等於沒有回歸防線。 -->

## TC-07 Wheel 物理行為（fling / snap）

- **Priority:** P1
- **Module:** TimerWheelPicker
- **Status:** Active

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | SETTING、minute wheel 顯示 | 慢拖一個 grid 高度然後放開 | wheel 移動 1 格、snap 到下一 grid | 👁 Manual（真實慢拖手感 + snap 視覺） | AC6；`unit:TimerWheelPickerTest#slow fling under threshold is consumed without scrolling`；acceptance `ac7_slowDragIsGridByGrid` |
| 2 | SETTING、minute wheel | 用力快速 fling 向上 | wheel 多滾數格（慣性），最後 snap 到 grid；最多 30 格內 | ✅ `InstFlow#fastFlingOnMinuteWheelMovesItAndSettlesWithinRange`（真機真實 fling：移動離格 + snap 整數 + 0..99 clamp）/ 👁 慣性手感 | AC7；`unit:TimerWheelPickerTest#fast fling is capped to MAX_FLING_GRIDS`；acceptance `ac7_fastFlingTriggersInertia` |

<!-- 示範點：「✅ 的界線 = 看路徑不看值」——sub 2 標 ✅ 因為真實 MotionEvent fling + snap 收斂是真機才有的路徑；fling 上限格數常數、clamp 數學等純邏輯留 unit，instrumented 不重複計。 -->

# 節錄：VB-385 Timer COUNTDOWN

## TC-09 跨 FinchService 完整接線 happy path（e2e）

- **Priority:** P0
- **Module:** TimerCountdown
- **Status:** Active

> **為何手動**：outer / VM test 各自 `new` SUT 組 fake、不經 `FinchService`；全綠仍可能漏掉整條未接的 production 接線。本 TC 在真機驗整條接線真的通，**刻意無 unit backstop**（unit 證明上搆不到 service 接線層）。

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | 真機 flag=true、Idle | Dashboard + chip → SETTING → 撥 wheel → Start | 倒數完整 UI **真的在 overlay 渲染**並逐秒遞減（非只 emit state） | 👁 Manual（FinchService→SETTING host→Countdown host 真實接線；unit 不可觀察） | VB-538 教訓（outer test 遮住 FinchService 接線缺口） |

<!-- 示範點：manual 也可以「刻意無 unit backstop」——但要在 TC 註記裡寫清楚為什麼 unit 證明上搆不到，而不是留空。 -->
