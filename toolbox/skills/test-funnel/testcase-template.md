<!--
  Test Case 文件模板 — 由 /toolbox:test-funnel 產出。
  格式萃取自 canonical 範例 edu-vbos-finch `test_case/freezer.md`（VB-579 / VB-882）。
  新增一份產品 test_case 檔時，從「# Test Case 維護規範」整段複製當表頭，
  再依 AC 數量複製檔末 `## TC-XX` 模板列。
-->

# Test Case 維護規範

編號規則：`TC-XX` 在各產品 section 內遞增，**只新增不重編**；廢棄的 TC 在標題標註 `(Deprecated)`，不要刪行也不要回收編號（避免外部 ticket 引用斷裂）。

新增 TC 時，複製檔末 `## _TEMPLATE` 區塊。

> **「測試方式」欄（驗證驅動，每列一個主狀態）** — 本文件主軸：以 **instrumented 真機測試** 把「原本要人工驗」的 sub-case 自動化；逐列照此欄行動。
>
> **每列判定流程（測試漏斗）**：
> 1. 純邏輯（無 UI 依賴、有可讀值）先下沉成 **unit test**，標到備註欄 `unit:`，當回歸防線。
> 2. 問「這條原本要人工操作驗嗎？」否（已被 unit 蓋、無需人工）→ 不佔主軸；是 → 進 3。
> 3. 問「能否用 instrumented 自動化這條手動驗？」能 + 成本合理 → 寫 instrumented（主驅動 `✅`）；成本 > 手動價值（視覺對 Figma、觸控路由難、罕見 device-state、真機 IPC 畫面內容）→ `👁 Manual` 並註明理由。
>
> **狀態符號（測試方式欄）**：
> - `✅ <Class#method>` — 已用 **instrumented 真機測試** 覆蓋（`:app:connectedDebugAndroidTest`，需裝置）。
> - `△ <Class#method>` — instrumented 部分覆蓋，括號註明未涵蓋點（通常剩視覺）。
> - `⬜ pending` — 可 instrument、尚未實作。
> - `👁 Manual` — 維持手動（純視覺/感知對 Figma，或 instrument 成本 > 手動價值；括號註明理由）。
> - `⏸ Deferred` — 可 instrument 但暫緩。
>
> **備註欄 `unit:<Class#method>`** — 該列底層邏輯已由 JVM 單元 / Robolectric（`:app:testDebugUnitTest`，快、免裝置、CI 可擋）覆蓋，作回歸防線；非本文件主驗證驅動。
>
> **測試碼別名**（每份文件頭宣告一次，之後欄位用短名）：`Inst#` = `<Feature>InstrumentedTest`（`app/src/androidTest`，真機）；`Smoke#` = `<Feature>SmokeTest`（`app/src/test`，Robolectric）。

---

# <Jira key 或產品名稱>（<Feature>）

共 N 組 test case。

**功能說明**：
> <一句話功能 + 範圍邊界（哪些是本次範圍、哪些沿用不改）>
>
> **實作位置**（非 greenfield 時列出關鍵檔）：
> - `<path/to/Activity.kt>`
> - `<path/to/engine>`
>
> **關鍵機制**：<影響測試策略的非顯而易見行為，例：非 OS 級定格、延遲截圖、離散級距>

**來源**：
- <Jira key `customfield_12203` AC1–ACn（抓取日期）>
- <PRD / Feature Description>
- <既有實作 — 行為以程式為準>
- <留言：補出的額外 TC 與其 rationale>

## TC-01 <測項標題>

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | <前置條件> | <操作步驟> | <預期結果> | <✅ Inst#method / 👁 Manual（理由）/ ⬜ pending> | <ACn；unit:Class#method（底層佐證）+ 其他註記> |

---

## 設計點（實作前須對齊）

- <影響時機 / 接線 / 重構的決策，逐條列出>

## 自動化盤點（<萃取 ticket>，<日期>）

> 本文件主軸：以 instrumented 真機測試把「原本要人工驗」的 sub-case 自動化（`✅`）；純邏輯下沉 unit 當回歸防線（`unit:`）；視覺/感知或 instrument 成本 > 手動者維持 `👁`。

**測試資產**

| 層級 | 檔案 | 跑法 | 覆蓋（主軸 / 佐證）|
|---|---|---|---|
| **真機 instrumented**（主驗證驅動）| `<Feature>InstrumentedTest`（N case）| `:app:connectedDebugAndroidTest`（需裝置；CI 不跑）| <TC 清單> |
| **unit / Robolectric**（回歸防線）| `<...>Test` | `:app:testDebugUnitTest`（快、免裝置）| <純函式 / 不變式 / 機制清單> |

**維持手動的項目**（成本 > 手動價值 / 純視覺，已標 `👁` + 理由）

- **對 Figma 視覺**：<TC 清單>
- **真機畫面內容 / overlay 計時**：<TC 清單>
- **真實系統觸發（機制已 unit 覆蓋）**：<TC 清單>
- **instrument 成本高**：<TC 清單 + 理由>

**已知量化 quirk（非 regression）**：<會影響 instrumented 穩定性的引擎行為 + 規避手法>

## _TEMPLATE
> 新增 TC 時複製下方整段，把 `XX`、`<...>` 換掉。編號接續同 section 最後一個 TC +1。

```
## TC-XX <測項標題>

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | <前置條件> | <操作步驟> | <預期結果> | <✅ Inst#method（instrumented 主驅動）/ 👁 Manual（理由）/ ⬜ pending> | <unit:Class#method（底層佐證）+ 其他註記> |
```
