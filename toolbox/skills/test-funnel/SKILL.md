---
name: test-funnel
description: Use when turning a feature's AC / PRD into a test-case document and triaging each case into a unit test, an instrumented (real-device) test, or manual — for the edu-vbos-finch :app module. Triggers — a Jira key, pasted AC, "build test cases", "automate the manual tests", "backfill unit tests", "which tests do I need". Trigger: /toolbox:test-funnel
trigger: /toolbox:test-funnel
---

# /test-funnel — AC → 測試漏斗（unit / instrumented / manual）

把一個 feature 的 AC / PRD 變成一份 **TC 表格文件**，並對每個 sub-case 做三層分流，**真的把測試碼寫出來**：純邏輯沉到 unit、能自動化的手動驗證升成 instrumented、只剩純視覺/感知的留 manual。

**核心隱喻 = 測試漏斗**。預設一切都是「手動驗」，再往漏斗下游拉：
1. **補齊 unit**（底層、最便宜、CI 可擋）— 純邏輯下沉成 JVM/Robolectric 測試，當回歸防線。
2. **把手動測試自動化**（中層、主驗證驅動）— 用 instrumented 真機測試取代「原本要人工跑」的 case。
3. **只留真手動**（頂層）— 純視覺對 Figma、真機畫面內容、或 instrument 成本 > 手動價值者。

> **範圍**：產出 TC 文件 + 寫測試碼。**AC 文字的 GWT 改寫委派 `/toolbox:gwt`；測試碼的命名/stack 慣例委派 `android-testing` skill。** 本 skill 是這兩者的編排層。

> **維護紀律：test-case 檔面向「現在的可維護性」，不是保存歷史。** 這條凌駕本檔其他格式建議。
> - **單一真實來源 = 每列的 `測試方式` / `備註` 欄。** 不要另立會 drift 的彙總段（「自動化盤點」「維持手動清單」「哪個檔覆蓋哪些 TC」）——那些是 per-row 的重複拷貝，TC 一改就走樣。「還有哪些手動」用 `grep '👁' <file>` 即時導出，不手維護。
> - **不再適用的測項直接移除**，不留 `(Deprecated)` placeholder、不留「原本…已改 / 行為定案（日期）/ 已修正」這類變更說明。TC 只描述**現在的** Given / When / Then；歷史交給 git。
> - **編號**：現存編號不重編、不回收（外部 ticket 可能引用）；移除後留下的編號空缺屬正常，**不補 placeholder**。
> - 加東西前先問：「這是描述**現狀**，還是在記**曾經發生**？」後者不寫進檔。

---

## When to invoke

- 使用者給 Jira key 或貼 AC，要「做出 test case」/「規劃要寫哪些測試」
- 使用者問某 feature「哪些該 unit、哪些該 instrumented、哪些只能手動」
- 使用者要「把手動測試自動化」或「補齊 unit test」
- 關鍵字：`test case / 測試案例 / 測試漏斗 / instrumented / connectedDebugAndroidTest / 自動化手動測試 / 測試方式欄 / 三層分類`

---

## 輸入與觸發

| 觸發 | 動作 |
|------|------|
| `/toolbox:test-funnel VB-1234` | 讀 ticket 的 AC / PRD（走 gwt 的 Step -1 Jira 抓法，見下） |
| `/toolbox:test-funnel "<直接貼 AC>"` | 直接吃貼進來的 AC 草稿 |
| `/toolbox:test-funnel`（無參數） | 問使用者要 Jira key 還是貼 AC |

---

## 流程

### Step 1 — 取得 AC

- Jira key → 用 `getJiraIssue`（`fields: ["summary","description","customfield_12203","comment"]`、`responseContentFormat:"markdown"`、cloudId `2ea8088c-133a-424f-9a3b-946e7ade9dad`），把 `customfield_12203` 當 AC 草稿。遵守全域 CLAUDE.md 的 Atlassian 慣例（預設只讀不寫）。
- 貼上的 AC → 直接用。

### Step 2 — 整理 AC（條件式委派 gwt）

先看 `test_case/<key-or-slug>.md` 是否已存在且含整理過的 GWT：

```
test_case/<key>.md 已有整理過的 AC（GWT / Rule）？
├── 有 → 直接吃，進 Step 3
└── 沒 → 引用 /toolbox:gwt 把 AC 改寫成 GWT + Rule，拿它的分類結果，再進 Step 3
```

> 不要自己重做 GWT 改寫——那是 gwt 的職責。本 skill 只在「還沒整理過」時調用它。

### Step 3 — 轉成 TC 表格文件

把整理過的 AC 落地成 **TC 表格文件**，格式用 `testcase-template.md`（本 skill 目錄；填好的乾淨範例見 `canonical-example.md`）。

- 路徑：`test_case/<key-or-slug>.md`（有 Jira key 用 key，否則 kebab-case slug）。`test_case/` 不存在就建立；同名檔先問覆蓋還是另存。
- 表頭整段「Test Case 維護規範」照抄（符號規範 + 每列判定流程）。
- 每條 AC → 一個 `## TC-XX`，sub-case 攤成表格列（`Sub | Given | When | Then | 測試方式 | 備註`）。
- 檔頭宣告測試碼別名：`Inst#` = `<Feature>InstrumentedTest`、`Smoke#` = `<Feature>SmokeTest`。

### Step 4 — 三層分流 + 寫測試碼

**逐列**走決策樹（這是漏斗的分流邏輯，每列只判一個主狀態）：

```
這一列（一個 sub-case）：

1. 是純邏輯、無 UI 依賴、有可讀值嗎？
   是 → layer (a)：抽成純函式 + 委派 android-testing 寫 unit/Robolectric
        備註欄標 unit:<Class#method>。（這層通常開發時已部分存在，補齊缺口。）

2. 「這條原本要人工操作驗嗎？」
   否（已被 unit 蓋、無需人工）→ 不佔主軸，測試方式欄留空或註明已 unit 覆蓋
   是 → 進 3

3. 「能否用 instrumented 自動化這條手動驗？」
   能 + 成本合理            → layer (b)：用 instrumented-harness.kt 寫真機測試
                              測試方式欄標 ✅ Inst#method
   成本 > 手動價值          → layer (c)：測試方式欄標 👁 Manual（理由）
     └─「成本 > 價值」的四個典型情境（直接當理由寫）：
        · 純視覺對 Figma         · 觸控路由難（按鈕吃手勢 / 可拖區極小）
        · 罕見 device-state       · 真機 IPC 畫面內容 / overlay 計時
```

> **`✅` 的界線 = 看路徑，不是看值**：判準**不是**「這個值能不能用 unit 算出來」，而是「從觸發到斷言，**中間那條路徑**需不需要真機才有的東西」——真動畫（wall-clock 內插）、真手勢（MotionEvent 路由）、真 IPC（截圖）、真 lifecycle / 真 looper 上的 async（如動畫收斂後才 postDelayed 翻 button state）。
> - **需要真機路徑 → `✅`**：即使該值另有純函式 unit backstop，兩者並存（backstop 標 `unit:`），不衝突。
> - **觸發→斷言之間只是可直接呼叫的純邏輯**（注入時鐘的時間運算、reset 的瞬間狀態寫入、clamp 幾何）→ instrumented 沒測到 unit 搆不到的東西，留 `unit:`，instrumented 至多 `⬜` smoke，**別標 `✅` 重複計**。
>
> 對照範例（zoom freezer 經驗）：**「到上限後放大 disabled」仍是 `✅`**——到上限要跑真動畫 `zoomInUntilSettled` + 等 `checkButtonState` 的 postDelayed 才翻 disabled，是真機 async 路徑；但純 `nextZoomScale` / clamp 數學只留 unit。同型範例見 `canonical-example.md` TC-07（真實 fling 標 `✅`、fling 上限常數留 unit）。**反例**：碼錶 pause/resume 累加只是注入時鐘的算術、無真動畫/手勢 → unit 就夠，別標 `✅`。

**動作（不是只標符號，是真的寫）**：
- **(a) unit**：把可測邏輯從 View / Activity 抽成純函式（行為等價，附帶生產端委派重構），委派 `android-testing` skill 寫測試。跑 `:app:testDebugUnitTest` 綠後，備註欄填 `unit:Class#method`。
- **(b) instrumented**：用 `instrumented-harness.kt` 的三個 pattern（ActivityScenario 拉非-exported Activity、手勢注入、settle-gating）寫真機測試。跑 `:app:connectedDebugAndroidTest`（需裝置）綠後，測試方式欄填 `✅ Inst#method`。
- **(c) manual**：測試方式欄填 `👁 Manual` + **具體理由**（用上面四情境），不留空白。

### Step 5 — 回填

- 每寫完一個測試，回填對應列的符號（`✅` / `unit:` / `👁`）。**回填就是收尾**——測試結果落在 per-row 欄位即可。
- **不要**另加文件尾的「自動化盤點」彙總段（covered-which-TC、哪些手動、quirk 清單）：那是 per-row 的重複拷貝、會 drift（見〈維護紀律〉）。要看整體覆蓋就 `grep` 符號欄即時導出。
- 寫檔前 echo 最終內容給使用者確認再落地。

---

## 測試方式欄 / 備註欄符號（速查）

| 欄 | 符號 | 意義 | 跑法 |
|----|------|------|------|
| 測試方式 | `✅ <Class#method>` | instrumented 真機已覆蓋（主驗證驅動）| `:app:connectedDebugAndroidTest`（需裝置）|
| 測試方式 | `△ <Class#method>` | instrumented 部分覆蓋（括號註未涵蓋點）| 同上 |
| 測試方式 | `⬜ pending` | 可 instrument、尚未實作 | — |
| 測試方式 | `👁 Manual` | 維持手動（括號註理由）| 人工 |
| 測試方式 | `⏸ Deferred` | 可 instrument 但暫緩 | — |
| 備註 | `unit:<Class#method>` | 底層邏輯已 unit/Robolectric 覆蓋（回歸防線，非主軸）| `:app:testDebugUnitTest`（快、免裝置、CI 擋）|

完整規範與 TC 模板：見 `testcase-template.md`。

---

## instrumented 的價值與紀律

instrumented 不是「再跑一次 unit」——它涵蓋 Robolectric 測不到、且為避 flake 刻意略過的層：**真實動畫**（wall-clock 內插）、**真實手勢**（注入 MotionEvent）、**真機 IPC**（截圖等）。`exported="false"` 的 Activity 用 `ActivityScenario` 在 app process（uid 1000）內合法拉起，等同把「原本手動 uiautomator 驅動」變成可重複自動測試。

**避 flake 鐵律**（settle-gating）：非同步狀態（動畫 / postDelayed / IPC）**不可用固定 sleep 斷言**——裝置負載高時固定 sleep 不夠。一律「輪詢到符合條件或逾時」；連續操作用收斂閘控（等前一動畫確實生效且兩次取樣不變才再做）。template 已內建 `waitValueNear` / `waitEnabled` / `waitReady` / `repeatUntilSettled`。

---

## Common mistakes

| 錯誤 | 修正 |
|------|------|
| 看到 AC 直接寫 instrumented | 先走決策樹：純邏輯該沉 unit，不是什麼都 instrument |
| 純邏輯也用 instrumented 測 | 慢、要裝置、CI 擋不住；抽純函式走 unit |
| 觸發→斷言只是純邏輯（含注入時鐘）卻標 `✅` 主驅動 | 看路徑不看值：無真動畫/手勢/IPC/lifecycle 就留 `unit:`，instrumented 至多 `⬜` smoke。反之若值被 unit 蓋但**到達該值要跑真機路徑**（如動畫到上限後 async 翻 button state），`✅` 與 `unit:` backstop 並存才對 |
| `👁 Manual` 不寫理由 | 必須註明四情境之一，否則無法判斷是真手動還是偷懶 |
| 自己重做 GWT 改寫 | 委派 `/toolbox:gwt`；本 skill 只在沒整理過時調用 |
| instrumented 用固定 sleep | 必 flake；用 settle-gating 輪詢/收斂閘控 |
| 在動畫未停時連點 | 中間值可能被判 off-grid 跳回起點；用 `repeatUntilSettled` |
| 重編 / 回收 TC 編號 | 現存編號不重編、不回收，避免外部引用斷裂 |
| 廢棄測項標 `(Deprecated)` 留著當 placeholder | 不再適用的測項**直接移除**（不留 placeholder / 不留歷史說明）；留下的編號空缺正常 |
| 加「自動化盤點」等彙總段重複 per-row | 單一來源是每列 `測試方式`/`備註` 欄；彙總會 drift。整體覆蓋用 `grep` 導出，別手維護 |
| 寫「原本…已改 / 行為定案（日期）」歷史註記 | TC 只描述現狀；變更歷史交給 git，不留在 test-case 檔 |

---

## Canonical 範例（皆在本 skill 目錄，不依賴外部 repo）

- `canonical-example.md` — **乾淨範例**（節錄自 edu-vbos-finch `test_case/timer.md`，VB-82 / 385 / 386）：純現狀、無彙總段、無歷史、廢棄項直接移除（符合〈維護紀律〉），含每個示範點的註解。
- `testcase-template.md` — 格式與決策樹的單一來源（維護規範表頭 + per-row 三層分流 + 符號規範 + TC 模板）。
- `instrumented-harness.kt` — instrumented 三 pattern（ActivityScenario / 手勢注入 / settle-gating；源自 IFP41 全綠 10 case 的 `ZoomActivityInstrumentedTest.kt`）。

## 相關 skill

- `/toolbox:gwt` — 上游：AC → GWT / Rule 改寫（Step 2 條件式調用）。
- `android-testing` — 下游：unit / instrumented 測試碼的命名與 stack 慣例（Step 4 委派）。
- `/toolbox:dual-loop-flow` — 大型 multi-PR feature 的縱切 + 雙循環 TDD 規劃。
