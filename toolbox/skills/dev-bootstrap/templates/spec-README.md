# {{PROJECT_NAME}} Canonical Product Docs — Index

> 「目前已 ship 的設計事實」canonical docs 文件群（單一 source of truth，扁平放在 `docs/product/`，無 `spec/` 子層）。
> 任何 brainstorm session 的 spec 稿（`docs/superpowers/specs/*.md`）一旦落地成 plan、plan ship 後，內容會被吸收進這裡的對應 canonical doc。

## 文件 lifecycle（四層）

```
docs/superpowers/backlog.md                             ← 粗略想法暫存區（一句話，刻意不寫細節）
       ↓ superpowers:brainstorming
docs/superpowers/specs/{YYYY-MM-DD}-{topic}-design.md   ← brainstorm 設計稿（spec 稿）
       ↓ superpowers:writing-plans
docs/superpowers/plans/{YYYY-MM-DD}-{topic}.md          ← 實作計畫（+ QA log）
       ↓ ship + user QA pass
docs/product/{topic}.md                                 ← canonical docs（吸收後刪 plan）
更新本檔索引 + 變更歷史
```

**不適用 lifecycle 收尾的情況**：純 bug fix、純文件清理、依然在實驗階段 — spec 稿留著、不吸收進 canonical。

## Commit-prefix convention

| Prefix | 對象 |
|---|---|
| `docs(spec):` | brainstorm 設計稿（`docs/superpowers/specs/`） |
| `docs(plan):` | 實作計畫（`docs/superpowers/plans/`） |
| `docs(canonical):` | canonical docs 更新（`docs/product/`） |

## 結構分層

| 區段 | 檔案 |
|---|---|
| Overview & 目標 | [overview.md](./overview.md) |

> Cross-cutting canonical docs（stack / architecture / ui-ia / schema / api / auth / ...）等到第一個相關 feature 落地時再建檔。不預建空 stub。

## Feature-level Docs（canonical）

| 功能 | 檔案 |
|---|---|
| _尚無 feature ship_ | — |

## 變更歷史

| 日期 | 變更 |
|---|---|
| {{TODAY}} | 初始化 — dev-bootstrap 建立 docs/product/ 結構與本索引 |
