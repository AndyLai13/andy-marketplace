<!--
  Append-mode block for dev-bootstrap.
  Pasted at the bottom of an existing README.md when README.md already exists.
  Does NOT include H1 or one-liner (the existing README already has those).
  Workflow block (solo / team) follows the same KEEP-IF-* convention as templates/README.md.
-->

---

## 開發協作規約

<!-- KEEP-IF-SOLO-START -->
**獨立開發專案** — 不開 PR、不做 code review handoff。Workflow：

1. `feat/*` 或 `fix/*` 分支做事、commit 多筆、E2E + 整合測試確認綠
2. 直接 merge 回 `main`（fast-forward 或 `--no-ff`，視 commit history 偏好）
3. `git push origin main` 上 GitHub
4. 不開 PR、不留 release branch

AI assistant 只需要交付乾淨可 merge 的 feature branch，不需要建議「開 PR / 等 review」。merge 由我手動或在對話中明確下指令時執行。
<!-- KEEP-IF-SOLO-END -->

<!-- KEEP-IF-TEAM-START -->
**團隊開發** — feature branch + PR review。Workflow：

1. `feat/*` 或 `fix/*` 分支做事、commit 多筆、E2E + 整合測試確認綠
2. 推上 GitHub、開 PR
3. 等 reviewer 簽核、CI 全綠
4. Merge 到 `main`、刪 feature branch（local + remote）

AI assistant **不要**直接 push 到 main、**不要** self-merge PR。完成 feature branch 後交付 PR 連結即停。
<!-- KEEP-IF-TEAM-END -->

### 任務完成 protocol

當我（user）說「本任務完成」/「任務完成」/同義詞，AI assistant 自動執行下列三步、不需要再問：

1. **整合 main**
   - 不管是否開 feature branch 處理：commit + push / merge 回 main + push
   - main branch 直接 commit 的：`git push origin main`
   - Feature branch 上做的：`git merge feat/xxx --no-ff` → `git push origin main`

2. **殘留 branch 刪除**
   - Merge 完成的 feature branch：**先** `git push origin --delete feat/xxx` **再** `git branch -d feat/xxx`（順序重要 — local 先刪 remote delete 會擋）
   - 沒 merge 但放棄的 branch：先跟 user 確認再刪

3. **文件生命週期收尾**
   - feature 對應的 `docs/superpowers/drafts/<date>-<slug>.md` 跟 `docs/superpowers/plans/<date>-<slug>.md` 蒸餾入 `docs/product/spec/<feature>.md`（canonical）
   - 原 draft + plan + QA log 刪除
   - 更新 `docs/product/spec/README.md` 索引 + 變更歷史一行
   - 不適用情況:純 bug fix、純文件清理、依然在實驗階段 — 這時 draft 留著、不建 spec

三步全 commit 後再回報 user。中間若遇衝突（merge conflict、push 失敗、未追蹤檔殘留）才停下來問。

### 等 review checkpoint（任務完成 protocol 的前置 gate）

Plan 最後一個 task 跑完、tests 過、給 user 視覺驗證項目後 → **STOP**。**禁止**自動：

- push 後續 lifecycle commit
- 刪 `docs/superpowers/drafts/*` / `docs/superpowers/plans/*` 檔
- 改 `docs/product/spec/README.md` 變更歷史
- 寫 spec 各檔的新段落

只在 user 明確說觸發詞之後，才執行上面三步。

**觸發詞**：「本任務完成」、「任務完成」、「這個功能完成了」、「ship 完了」、「驗證完畢」、「OK 收尾」
**不觸發**：「告一段落」、「先 commit」、「暫停」、「先停在這」

---

## 文件結構

```
docs/
├── product/
│   ├── spec/            canonical 設計事實（單一 source of truth）
│   │   ├── README.md    spec 索引 + lifecycle 規約 + 變更歷史
│   │   ├── overview.md  產品高層總覽
│   │   └── *.md         功能級 spec（feature ship 後加）
│   └── testing/         測試 doctrine + pattern + 當前快照（若 bootstrap 時啟用）
│       ├── README.md    索引（連結 test-strategy skill）
│       ├── shape.md     tier 比例目標 + 不投資清單
│       ├── patterns.md  Mock / RPC race / module 抽取樣板
│       ├── coverage.md  v8 跨 process 限制 + 門檻
│       ├── ci.md        CI 政策 + fileParallelism 警示
│       ├── multi-tenant-safety.md  RLS cross-leak（critical）
│       ├── status.md    當前 snapshot（覆蓋更新）
│       └── test-pyramid-shapes.svg 視覺化
├── superpowers/
│   ├── drafts/          brainstorm 階段（feature ship 後刪）
│   ├── plans/           實作 plan + QA log（feature ship 後刪）
│   └── runbooks/        運維/手動操作手冊（永久）
└── reference/
    └── glossary.md      詞彙手冊
```

文件 lifecycle：brainstorm draft → implementation plan → ship → 蒸餾入 `product/spec/<feature>.md`，draft + plan 隨之刪除。詳見 [`docs/product/spec/README.md`](docs/product/spec/README.md)。

### 檔名 convention

| 類型 | Pattern |
|---|---|
| brainstorm draft | `docs/superpowers/drafts/{YYYY-MM-DD}-{topic}.md` |
| implementation plan | `docs/superpowers/plans/{YYYY-MM-DD}-{topic}-implementation.md` |
| QA fix log | `docs/superpowers/plans/{YYYY-MM-DD}-qa-fixes-log.md` |
| canonical feature spec | `docs/product/spec/{feature}.md` |

---

## QA 修 bug workflow

手動 QA session 撞 bug 時：

1. 用 background subagent 修，主對話**不夾雜**診斷 / 實作細節
2. 修復細節（root cause / 檔案 / commit / 測試結果）寫到當天的 QA log：`docs/superpowers/plans/{YYYY-MM-DD}-qa-fixes-log.md`
3. 主對話只保留三類：QA 步驟指示、user 的 pass/fail 回報、subagent 完成後的一行通知（例「✅ ISSUE-XX 修了，請 hard-refresh 重測」）
4. 想看細節 → user 自己讀 QA log
5. QA log 在 lifecycle 收尾時一起刪
