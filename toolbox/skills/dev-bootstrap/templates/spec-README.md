# {{PROJECT_NAME}} Product Spec — Index

> 「目前已 ship 的設計事實」canonical 文件群。
> 任何 brainstorm session 的 `docs/superpowers/drafts/*.md` 一旦落地，內容會被吸收進這裡的對應檔，draft 與 plan 同步刪除。

## 文件 lifecycle

```
brainstorm session
       ↓
superpowers/drafts/{YYYY-MM-DD}-{topic}.md          ← 草稿
       ↓
寫 plan
       ↓
superpowers/plans/{YYYY-MM-DD}-{topic}-implementation.md
       ↓
Plan 執行完 ship + user QA pass
       ↓
吸收內容進 product/spec/{topic}.md (canonical)
更新本檔索引 + 變更歷史
刪除 plan + 刪除 draft + 刪除 QA log（git history 已保存）
```

**不適用 lifecycle 收尾的情況**：純 bug fix、純文件清理、依然在實驗階段 — draft 留著、不建 spec。

## 結構分層

| 區段 | 檔案 |
|---|---|
| Overview & 目標 | [overview.md](./overview.md) |

> Cross-cutting spec（stack / architecture / ui-ia / schema / api / auth / ...）等到第一個相關 feature 落地時再建檔。不預建空 stub。

## Feature-level Specs

| 功能 | 檔案 |
|---|---|
| _尚無 feature ship_ | — |

## 變更歷史

| 日期 | 變更 |
|---|---|
| {{TODAY}} | 初始化 — dev-bootstrap 建立 spec/ 結構與本索引 |
