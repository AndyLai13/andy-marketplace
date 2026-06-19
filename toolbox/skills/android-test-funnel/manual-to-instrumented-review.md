# 手動測項 → instrumented 升級 review 準則（android-test-funnel 反向模式）

> android-test-funnel 的**反向 / audit 方向**：不是從 AC 往下分流，而是拿一份**已存在**、結論寫了
> `👁 Manual` / `⏸ Deferred` 的 test_case 文件，**挑戰那些結論**、能翻就升成 instrumented。
> 方法論可泛化，實作 lore 對準 edu-vbos-finch（漏斗符號 ✅/△/⬜/👁/⏸ 見 `testcase-template.md`）。
>
> **settle-gating / 禁固定 sleep 不在此檔重述**——見 SKILL.md〈instrumented 的價值與紀律〉，
> 本檔只補它沒講的 audit 判斷與實作踩坑。

## 0. 觸發時機

- test_case 文件存在 `👁 Manual` / `⏸ Deferred` / 「architecturally 不可 instrument」結論
- **技術前提變動時必須重新 review 既有結論**：新 a11y 屬性落地、新測試基建（如共用 e2e base）、
  對平台機制的理解被修正（如 shell dump ≠ instrumented UiAutomator）
- 文件結論有時效性——它記錄的是「當時的工具與屬性狀態」，不是物理定律

## 1. 先驗證前提，不接受文件結論（spike first）

「不可自動化」結論背後是一條推論鏈；逐環檢查哪環是**工具限制**、哪環是**架構限制**：

- 實例：「`FLAG_NOT_FOCUSABLE` → `uiautomator dump` 抓不到 → UiAutomator 不可用」
  ——錯在中環：shell `uiautomator dump` 只 dump focused window；**instrumented test 內的
  `UiDevice` 列舉所有 interactive windows**（`FLAG_RETRIEVE_INTERACTIVE_WINDOWS`）。
- 實例：「toolbar a11y-hidden → By.res 抓不到」——UiAutomator 預設帶
  `FLAG_INCLUDE_NOT_IMPORTANT_VIEWS`，其實看得到（但 TalkBack 看不到，補 desc 仍是對的）。
- 用**最小 spike** 實證三件事：找得到（By.res/desc）、點得下（click/swipe 真的進 window）、
  window 層讀得到（`uiAutomation.windows` 的 layer/bounds）。先 falsify 再投資。
- spike 必須在**真實掛載方式**上做（同 window type / flags / process），不是 host-activity 替身。

## 2. 封鎖原因分類學

對每列 👁，先問「它到底被什麼擋住」，再對照下表（與 SKILL.md Step 4「成本 > 價值」四情境互補，
這裡是更全的硬底線清單）：

### 可翻案類（工具 / 屬性問題，補基建即解）

| 封鎖理由 | 解法 |
|---|---|
| a11y-hidden（`importantForAccessibility="no"`）| 拔 suppression + 補 contentDescription（順帶修 TalkBack） |
| 無 resource-id / desc（程式化 view）| 程式化補 desc（單一來源原則：在 view 建構處設） |
| 「跨 process」 | 只擋 ActivityScenario / Espresso；**UiAutomator 本就跨 process** |
| 「WindowManager overlay 搆不到」 | instrumented UiAutomator 可達（NOT_FOCUSABLE 也在 a11y window list） |
| 「焦點被外部 app 搶」 | 環境問題：`@Before` 內 `executeShellCommand("am force-stop <pkg>")` 程式化排除 |
| 「真實系統觸發」（Home / back） | `UiDevice.pressHome()/pressBack()` 就是真按鍵注入 |
| 「overlay 提示出現 / 計時」（Snackbar） | `Until.findObject` + `Until.gone` 計時窗 |
| 「z-order / window root 順序」 | `uiAutomation.windows` 每個 window 有 layer，可程式比較 |
| 「真實手勢 / 慣性結果」 | `UiObject2.drag/swipe/click` + bounds 斷言（手感本身仍 manual） |
| 「e2e 跨 window flaky」（⏸ 常見理由） | 真 service + 焦點小偷排除 + Until 輪詢（不用固定 sleep）後通常消失 |

### 硬底線類（本質不可自動化，誠實保留 manual）

- **canvas 自繪、無 a11y 節點**：點得到但斷言不了內部 state（除非補 `ExploreByTouchHelper` 虛擬節點——另立工程）
- **對 Figma 純視覺**：配色、弧度、光暈、bracket 樣式（一次性資產正確性）
- **真實音訊**：人耳辨音色、可聽性
- **主觀手感**：拖曳跟手、fling 慣性感
- **跨 app 互通**：對方 app 行為非本 repo 可控
- **跨 process kill 自己**：instrumentation 與 app 同 process，force-stop 自家 = 自殺
- **長時間 wall-clock 漂移**
- **「first-run」前置**：DataStore 等持久化跨 run 殘留，無法保證初次條件

## 3. 升級主張的證據鏈

- 每列升級 = **e2e 實跑綠** + **文件標記同步**（✅/△ + 測試碼別名 + 未涵蓋點）
- `△` 的紀律：括號寫清楚「驗了什麼、**沒驗什麼、沒驗的由誰守**（`unit:` / manual）」
- 同型變體（5/10/15 chip 三胞胎）：驗一個標 ✅，其餘標 `⬜ pending（同型，X 變體已 ✅）`，不重複實作
- **翻案直接改現狀符號（👁 → ✅），不在 TC 檔留時代註記**：日期 / 依據 / 「哪半邊舊結論仍成立」
  寫進**翻案那個 commit 或 PR 描述**，TC 檔維持現狀-only（見 SKILL.md〈維護紀律〉，凌駕一切）。
- spec / 文件 / production 衝突時：**已人工驗收的 production 行為為準**，向上修文件，
  引 archived change 的決議紀錄當依據（同樣寫進 commit，不留 TC 檔）。

## 4. 測試實作準則（實戰踩坑清單）

1. **斷言對準 production 真實 sink**——先讀實作再寫斷言。教訓：save 是直接 `FileOutputStream`
   寫 `Pictures/`、不經 MediaStore；用 MediaStore 查詢「綠燈」其實是無過濾誤中他源 row。
2. **負向斷言禁用 `Until.gone`**——目標當下不存在時 gone 立即回 true，慢半拍出現的 regression
   會 false-pass。寫法：`assertFalse(device.wait(Until.hasObject(...), N) ?: false)`。
3. **查共用資源必過濾 + 必清理**——檔案系統 / MediaStore 查詢加 owner / 檔名 pattern / 時間窗
   過濾；清理放 `@After`（失敗路徑也不殘留），不放測試結尾。
4. **測試錨點走 string resource**——contentDescription 同時是 TalkBack 文案與測試 selector，
   hardcode 字面值 = 雙真相，翻譯一動全批假性失敗。
5. **全域單例改了要還原**——`uiAutomation.serviceInfo` 是 instrumentation 級單例，
   `@Before` 改 flags、`@After` 還原。
6. **e2e 走 production wiring**——真 service 啟動、真 overlay 掛載；不要 new SUT 組 fake
   （outer test 遮住接線缺口的教訓）。
7. **機制 vs 值分層**——e2e 驗「路徑通」（方向、出現、轉場），精確值留 unit
   （fling 只 assert 方向，倍率由純函式 unit 鎖）。對應 SKILL.md「看路徑不看值」判準。
8. **環境前提程式化**——force-stop 焦點小偷寫進 `@Before`，不靠人工 SOP；測試自包含
   才能進全套。
9. **production 修改一律 TDD 紅→綠**——即使是測試工程途中挖出的 fix（如 facade null 防護）。
10. **canvas 控件繞道**——play/pause 在 a11y-suppressed dial 上時，改走等價的標準按鈕路徑
    （SETTING→Start），或用 ready 態驗 no-op；不硬上座標。

## 5. 驗證紀律

- **每批升級單獨跑綠再進下一批**（emulator 逐批驗證）
- **共用 base 改動後，所有使用者 class 全部重跑**
- **真機重驗為終局**——emulator 綠 ≠ 真機綠；兩邊 z-order 等實測值要比對
- e2e 失敗先分類再動手：
  - **產品 bug**（賺到——升級成 production fix + TDD；這是 review 的一級產出不是雜訊）
  - **測試斷言錯**（對準真 sink 重寫）
  - **環境暫態**（emulator 閒置後第一輪可能要暖機：手動起 service 確認自癒 → 重跑）
- 跑測試前確認 APK 已重裝（改了測試碼卻跑舊 APK 的結果無意義）

## 6. 基建模式（可複製的工程資產）

- **共用 e2e base class**（`RealFinchOverlayUiAutomatorTestBase`）：service 啟停、焦點小偷排除、
  a11y flags 設定+還原、panel 開合 / 點擊 helper、window layer 工具——新工具的 e2e 只寫業務步驟
- **desc 錨點 lazy 自 string resource**
- **`@After` best-effort 善後鏈**（`runCatching` 逐層關，不互相連鎖失敗）
- **capability fallback 斷言**（emulator 無 VS 截圖 → screencap fallback，修好 facade 後
  emulator 也能跑真截圖流程）

## 7. 產出物 checklist（一輪 review 收尾時自查）

- [ ] 每列升級有對應綠燈測試 + 文件標記
- [ ] 翻案依據寫進 commit / PR（**不**留在 TC 檔；TC 檔現狀-only）
- [ ] 挖出的 production bug 已 TDD 修復並單獨 commit
- [ ] 維持 manual 的列有明確「本質原因」（對照 §2 硬底線類）
- [ ] 共用基建改動後全量重跑
- [ ] 真機重驗清單留交接（哪些 emulator-only）
