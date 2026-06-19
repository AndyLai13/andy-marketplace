<!--
  Test Case 文件模板 — 由 /toolbox:android-test-funnel 產出。
  表頭規範（編號 / 維護紀律 / 測試漏斗 / 狀態符號 / _TEMPLATE）的單一真實來源 =
  產出 repo 的 test_case/CONVENTIONS.md，本檔不再內嵌、避免 drift。
  新增一份產品 test_case 檔時（骨架與規則見 CONVENTIONS.md〈每檔表頭該放什麼〉）：
    1. 確保 test_case/CONVENTIONS.md 存在（缺檔才依其結構建立，不覆蓋既有）。
    2. 檔頭照下方骨架：H1 文件標題 → 導讀 blockquote → ## 測試碼別名 表 →（偏離通用才寫）## 架構限制。
    3. body 不寫「共 N 組」計數、不寫來源〈留言〉等歷史；單 feature 檔不立重複的 # <key> H1。
    4. 依 AC 數量複製 CONVENTIONS.md 的 ## _TEMPLATE 區塊。
  填好的乾淨範例見同目錄 canonical-example.md。
-->

## 檔案骨架（每份產品檔照放，不複製規範本文）

# <Jira key> (<Feature>) 測試案例

> 讀法、欄位意義、狀態符號、編號規範見 [`./CONVENTIONS.md`](./CONVENTIONS.md)；測試碼短名見下方〈測試碼別名〉。

## 測試碼別名

| 短名 | 測試類別 | 說明 |
|---|---|---|
| `Inst#` | `<Feature>InstrumentedTest` | `androidTest`，真機 instrumented 主驅動 |
| `Smoke#` | `<Feature>SmokeTest` | `test`，Robolectric 佐證 |

## 架構限制

<僅在偏離通用假設時才寫，例「全 UI 是 FinchService host overlay、無 Activity 可 `ActivityScenario` 拉起」「入口在 main process、介面在 `:tools` process」；通用情況整段省略>

---

**功能說明**：
> <一句話功能 + 範圍邊界（哪些是本次範圍、哪些沿用不改）；命名對照（內部名 vs 對外名）>
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

## TC-01 <測項標題>

- **Priority:** P0 | P1 | P2
- **Module:** <模組名稱>
- **Phase:** <選填，1 / 2 / ...>
- **Status:** Active

| Sub | Given (前置條件) | When (操作步驟) | Then (預期結果) | 測試方式 | 備註 |
|---|---|---|---|---|---|
| 1 | <前置條件> | <操作步驟> | <預期結果> | <✅ Inst#method / 👁 Manual（理由）/ ⬜ pending> | <ACn；unit:Class#method（底層佐證）+ 其他註記> |

---

## 設計點（實作前須對齊）

- <影響時機 / 接線 / 重構的決策，逐條列出>

## _TEMPLATE

> TC 模板與符號定義見 [`test_case/CONVENTIONS.md`](./CONVENTIONS.md) 的 `## _TEMPLATE` 與〈狀態符號〉節（單一真實來源，不在此重複）。
