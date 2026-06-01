---
name: gwt
description: 改寫 / 檢查 Acceptance Criteria 的 Given-When-Then 結構與具體度。Trigger: /toolbox:gwt
trigger: /toolbox:gwt
---

# /gwt — Acceptance Criteria 結構與具體度

把使用者貼進來的 AC 草稿改寫成「結構正確、具體可驗證」的 Given-When-Then + Rule 形式，並回報哪些地方仍不夠具體。

> **目前範圍**：只處理 AC 文字（從 Jira / Confluence / 對話 paste 進來）。**不處理測試碼**——測試方法命名、stack 細節走 `android-testing` skill。

---

## When to invoke

- 使用者貼了一段 AC / 驗收條件草稿
- 使用者要把功能描述「轉成 GWT」
- 使用者問某條 AC 是否「夠具體 / 可驗證 / 寫得對」
- 出現關鍵字：`驗收條件 / AC / acceptance criteria / GWT / Given-When-Then / Rule + scenario / 不變式 / invariant / scenario / 違反路徑`

---

## 互動流程

1. 取得 AC 來源——三種輸入方式擇一：
   - 使用者直接 paste AC 草稿
   - 使用者給 Jira key（例：`VB-82`）→ 走 **Step -1（從 Jira 抓 AC）**
   - 使用者給 Confluence 頁面 → 用 `getConfluencePage` 抓（同樣帶 `responseContentFormat: "markdown"`）
2. 先問：**「要全部改寫，還是只挑某幾條改？只做分類分析，還是要直接產出最終版？」**
3. 套用 Step 0 → Step 1 → Step 2 → Step 3
4. 回傳：改寫結果 + **具體度警示清單**（哪些 Given/When/Then 仍不夠具體、哪些 sub-case 缺漏）
5. 定稿後**寫成本地 test case 檔**（Step 4）——**不回填 Jira**
6. 不擅自填使用者沒提到的細節——不確定處標 `<待確認: ...>` 讓 user 補

> **不碰 Jira write**：本 skill 只「讀」Jira（Step -1 抓 AC），**絕不寫回**。定稿一律落地成本地 Markdown TC 檔，要不要同步回 ticket 由 user 自己決定。

---

## Step -1 — 從 Jira 抓 AC（僅當使用者給 issue key 時）

用 `mcp__plugin_atlassian_atlassian__getJiraIssue`，遵守 user 全域 CLAUDE.md 的 Atlassian MCP 慣例。

**預設呼叫參數**（VB 專案）：

```json
{
  "cloudId": "2ea8088c-133a-424f-9a3b-946e7ade9dad",
  "issueIdOrKey": "<VB-XXX>",
  "fields": ["summary", "description", "customfield_12203", "comment"],
  "responseContentFormat": "markdown"
}
```

**欄位對照**：
- `summary` — 標題（判斷需求脈絡）
- `description` — User Story / 功能描述（決定 Step 0 主詞/動詞/結果是否齊備的背景）
- `customfield_12203` — **VB 專案 AC 欄位**（本 skill 主要操作對象）
- `comment` — 留言（可能含 PM/QA 補充細節，會佔 context）

**Context 控制**：
- 第一次抓**先帶 `comment`**；若回傳超過 ~3K tokens（留言很多），下一次 retry 時把 `comment` 拿掉。
- 若 user 主動要看其他欄位（status / assignee / labels / priority…）再個別補加，不要預設拉滿。
- 不要用沒帶 `fields` 的呼叫——server 預設回 30+ 欄位、ADF JSON 形式，會立刻吃掉 8–15K context。

**抓完後**：把 `customfield_12203` 內容（Markdown 形式）視為 AC 草稿，直接進 Step 0。

---

---

## Step 0 — 入場門檻檢查（halt-or-proceed）

進到 Step 1 之前，**逐條 AC** 先檢查最低資訊是否齊備。三要件：

| 要件 | 判斷 | 範例 |
|------|------|------|
| **主詞** | AC 描述的對象（哪個系統、哪個 user role、哪個 UI 元件） | 「Owner 使用者」「首頁」「Manager App」 |
| **動詞 / 行為** | 發生什麼事或要做什麼 | 「長按」「啟動」「重啟」「設定」 |
| **可觀察結果** | 完成後能看到 / 量到的具體現象 | 「顯示對話框」「圖示消失」「Toast 出現」 |

**判斷規則：**

```
三要件齊全              → 進 Step 1
缺 1 件（其餘 2 件具體） → 進 Step 1，但缺的那件用 <待確認> 標出
缺 2 件以上 / 全部模糊   → HALT，不要進 Step 1
```

**HALT 時的回應格式**（不要硬擠模板、不要臆測填補）：

```
⛔ AC <編號> 資訊不足以分類，先釐清再改寫。

原文：「<逐字引用>」

缺少：
- <主詞 / 動詞 / 結果>：<說明缺什麼>

最可能的詮釋（請挑一個或補充）：
1. <詮釋 A 的具體版本>
2. <詮釋 B 的具體版本>
3. <詮釋 C 的具體版本>     ← 最多列 3 個

請選擇或提供其他細節，我再進 Step 1。
```

**踩煞車的訊號**：
- 整條 AC 只剩抽象形容詞（「應該好用 / 順暢 / 友善 / 穩定 / 安全」）
- 沒有具體 user action 或 system event
- 結果是抽象品質詞而非可觀察事件
- 「同上」「依規格」「比照競品」這類無自含資訊的引用

**Step 0 不要做的事**：
- 不要替 user 把模糊 AC 自動補成自己的詮釋
- 不要列超過 3 個詮釋（太多會變成你在替 user 設計需求）
- 不要在缺 2 件以上時還硬塞進 Step 1

---

## Step 1 — 分類每條 AC

依關鍵字 + 語意分四類，**先分類再套模板**，不要看到 AC 就直接 GWT：

| 類型 | 判斷依據 | 套用模板 |
|------|---------|---------|
| **Invariant** | 含「始終 / 永遠 / 不能 / 任何情況下 / 一定 / 絕不 / 保證」**且無合法繞過** | Rule + 違反路徑 scenario |
| **Default + override** | 有預設行為，**且存在合法繞過** | Default GWT + Override GWT |
| **State transition** | 單一觸發事件造成狀態改變 | 單條 GWT |
| **Pure value** | 預設值 / 「首次啟動 → 顯示 X」 | 單條 GWT |

判斷流程：

```
看到「始終 / 永遠 / 不能 / 任何情況下 / 保證」？
├── 是 → 有沒有合法繞過？
│   ├── 沒有 → Invariant (Rule + 違反路徑)
│   └── 有   → Default + override (兩條 GWT)
└── 否 → 有單一觸發事件嗎？
    ├── 有 → State transition (單條 GWT)
    └── 沒，是預設值/初值 → Pure value (單條 GWT)
```

---

## Step 2 — 套模板

### A. Invariant → Rule + 違反路徑

```
Rule: <一句話宣告，主詞 + 動詞 + 受詞，不留模糊空間>
範圍 / 名詞定義: <涉及的實體、邊界條件>
違反路徑 scenarios:
  1. Given <故意違反規則的前置> When <觸發> Then <invariant 仍成立>
  2. Given <另一條違反路徑>     When <觸發> Then <invariant 仍成立>
```

⚠️ 套用後，其他 GWT scenario 的 `Then` **不應再重複該 invariant 條目**。

### B. Default + override → 兩條 GWT

```
Scenario (default):
  Given  <未設定 override 的前置>
  When   <觸發>
  Then   <預設行為>

Scenario (override):
  Given  <已設定 override 的前置>
  When   <觸發>
  Then   <override 後的行為>
```

### C. State transition → 單條 GWT

```
Scenario:
  Given  <具體前置狀態>
  When   <單一觸發事件>
  Then   <可觀察的結果>
```

### D. Pure value → 單條 GWT

```
Scenario:
  Given  <初始條件，例：首次啟動 / 全新安裝>
  When   <讀取 / 開啟畫面>
  Then   <出現具體預設值，例：顯示 "Hello"、語系為 zh-TW>
```

---

## Step 3 — 具體度檢查（核心輸出）

每條改寫後的 scenario / Rule 都必須過這份 checklist。回覆使用者時要明確列出**通過/未通過項**，未通過的給出具體建議。

### Given 具體度

- [ ] 用具體值，而非抽象詞——**不接受**「正常狀態下 / 適當情況 / 使用者已準備好 / 一切就緒」
- [ ] 列出實際 actor（哪個 user role、哪個 app 狀態、哪個 device 模式）
- [ ] 列出可重現的前置（要設哪個設定、要按哪個按鈕、要進哪個畫面、版本/語系/帳號類型）

### When 具體度

- [ ] **只有一個**觸發事件——看到第二個 `And` 就拆 scenario
- [ ] 是行為動詞（「使用者長按 App 圖示 2 秒」）而非狀態描述（「圖示處於選取狀態」）
- [ ] 觸發來源明確（user gesture / system event / IPC call / timer）

### Then 具體度

- [ ] 結果**可觀察**——能在畫面上看到、能被測試斷言、有具體 UI 元素或 data field
- [ ] **沒有條件式**——`Then it should X if Y` 必須拆兩條 scenario
- [ ] **不重複** invariant 應該放在 Rule 的條目
- [ ] 對外可見的數量 / 文字 / 狀態值給具體值，而非「適當數量」

### 整體覆蓋率（TC-07 教訓）

- [ ] AC 標題承諾的每個保證，都有對應 scenario？
- [ ] Rule 的每條違反路徑都有獨立 scenario？最容易違反的情境有沒有寫？
- [ ] 名詞 / 動詞跨 scenario 一致？（例：Google service vs Google Services 不混用）
- [ ] 邊界條件涵蓋（reboot、登入/登出、guest 模式切換、orientation change、低 RAM kill 後重開）

---

## Reviewer 紅旗清單

掃描既有 AC 時，看到下列任一條就**停下來重寫**：

1. `When` 出現 2+ 觸發事件
2. `Then` 出現條件式（if / when 子句）
3. 同一句不變式在多條 `Then` 重複出現 → 改 Rule
4. `Given` 用「正常情況下 / 適當地 / 準備好 / 一切就緒」這類抽象描述
5. `And` 在 `Given` 鏈到 3 個以上前置條件
6. 名詞 / 動詞跨 scenario 不一致
7. AC 標題承諾 N 件事，sub-case 覆蓋 < N 件

---

## Anti-patterns（before → after 微範例）

### Pattern 1 — Invariant 散落在每條 Then

❌ Before
```
Scenario 1: ... Then 顯示首頁 And Google 服務必須存在
Scenario 2: ... Then 進入設定 And Google 服務必須存在
Scenario 3: ... Then 切換使用者 And Google 服務必須存在
```

✅ After
```
Rule: Google 服務在任何使用者狀態下都必須出現在首頁
違反路徑 scenarios:
  1. Given Manager 設定排除 Google     When 重啟          Then 仍顯示
  2. Given 使用者長按嘗試移除          When 鬆手          Then 圖示自動還原
  3. Given Guest 模式                  When 進入首頁       Then 仍顯示
```

### Pattern 2 — 條件式 Then

❌ Before
```
Then 顯示成功提示，如果網路可用，否則顯示錯誤訊息
```

✅ After
```
Scenario A: Given 網路可用     ... Then 顯示「同步成功」Toast
Scenario B: Given 網路不可用   ... Then 顯示「無網路連線」對話框
```

### Pattern 3 — 抽象的 Given

❌ Before
```
Given 使用者已正確登入並準備好操作
```

✅ After
```
Given 使用者已用 Google 帳號登入，且位於「我的應用程式」頁
When  使用者長按 Chrome 圖示 2 秒
```

### Pattern 4 — 多觸發事件擠在 When

❌ Before
```
When 使用者進入設定 And 點擊「應用捷徑」And 切換開關到 ON And 返回首頁
```

✅ After（拆成兩條 scenario，把中間步驟搬到 Given）
```
Scenario 1 (開啟設定):
  Given  位於設定 → 應用捷徑頁
  When   切換開關到 ON
  Then   出現確認對話框

Scenario 2 (套用至首頁):
  Given  應用捷徑開關已 ON
  When   返回首頁
  Then   首頁出現所選 4 個捷徑圖示
```

---

## Step 4 — 產出本地 test case 檔

改寫定稿後，**一律把結果落地成本地 Markdown TC 檔**（不回填 Jira）。

**路徑 / 命名**：`test_case/<key-or-slug>.md`
- 有 Jira key → 用 key：`test_case/VB-82.md`
- 純文字輸入 → 用 kebab-case slug（取自需求標題）：`test_case/app-shortcut-toggle.md`
- `test_case/` 目錄不存在就先建立；同名檔已存在先問 user 要覆蓋還是另存。

**檔案內容結構**（沿用「輸出格式」樣板 + 來源標頭）：

```markdown
# <Jira key 或標題> — Acceptance Criteria (GWT)

> 來源：<Jira key / Confluence URL / 對話 paste>
> 產出：/toolbox:gwt

## 分類
- AC1：<類型>（理由：...）

## 改寫
### AC1
<套用模板後的 Rule / GWT scenario>

## 具體度警示
- AC1 Given：<不夠具體之處 + 建議>

## 待確認
- <待確認: ...>
```

**注意**：
- 本檔是 **AC / GWT 文件**，不是測試碼。測試方法命名、stack 細節仍走 `android-testing` skill。
- 寫檔前先 echo 一次最終內容給 user 確認，再落地。

---

## Canonical examples

完整版範例（含覆蓋率清單）參考：

- `test_case/phase2.md` — VB-16 / VB-18 / VB-23 完整 TC
- 既有 memory：`feedback_invariant_not_gwt.md`（VB-18 AC7 改寫案例）

---

## 輸出格式（給使用者的回覆樣板）

```
## 分類
- AC1：<類型>（理由：...）
- AC2：<類型>（理由：...）

## 改寫
### AC1
<套用模板後的內容>

### AC2
<套用模板後的內容>

## 具體度警示
- AC1 Given：<不夠具體之處 + 建議>
- AC2 Then：<紅旗點 + 建議>

## 待確認（請補資訊）
- <待確認: AC1 中「常用 App」的判定條件是什麼？>
- <待確認: AC2 reboot 後是否保留設定？>
```
