# {{PROJECT_NAME}} — AI Assistant Cheat Sheet

完整規約見專案根 [`README.md`](./README.md)。以下是 AI assistant 必須遵守的核心契約摘要。

## 觸發詞辨識

**觸發任務完成 protocol**：「本任務完成」/「任務完成」/「這個功能完成了」/「ship 完了」/「驗證完畢」/「OK 收尾」

**不**觸發：「告一段落」/「先 commit」/「暫停」/「先停在這」

## 等 review checkpoint（任務完成 protocol 的前置 gate）

Plan 最後一個 task 跑完、tests 過、給 user 視覺驗證項目後 → **STOP**。

**禁止**自動執行：
- push 後續 lifecycle commit
- 刪 `docs/superpowers/specs/*` / `docs/superpowers/plans/*` 檔
- 改 `docs/product/README.md` 變更歷史
- 寫 canonical docs 各檔的新段落

只在 user 明確說觸發詞之後才執行下面三步。

## 任務完成三步（順序固定）

1. **整合 main**
   - main 直接 commit → `git push origin main`
   - Feature branch → `git merge feat/xxx --no-ff` → `git push origin main`

2. **殘留 branch 刪除**
   - Merged：**先** `git push origin --delete feat/xxx` **再** `git branch -d feat/xxx`（順序重要 — local 先刪 remote delete 會擋）
   - Abandoned：先跟 user 確認再刪

3. **文件 lifecycle 收尾**
   - 蒸餾 `docs/superpowers/specs/{date}-{slug}-design.md` + `plans/{date}-{slug}.md` + `plans/{date}-qa-fixes-log.md`（若有）入 `docs/product/{feature}.md`
   - 更新 `docs/product/README.md` 索引（若新檔）+ 變更歷史一行
   - 刪 spec 稿 + plan + QA log

三步全 commit 後才回報 user。中途 conflict / push fail / 殘留檔才停下問。

**不適用 lifecycle 收尾**：純 bug fix / 純文件清理 / 還在實驗階段（spec 稿留、不吸收進 canonical）。

## QA 修 bug workflow（手動 QA 撞 bug 時）

- 用 background subagent (`Agent` tool, `run_in_background: true`) 修
- 細節寫 `docs/superpowers/plans/{YYYY-MM-DD}-qa-fixes-log.md`
- 主對話只留：QA 步驟指示、user pass/fail 回報、subagent 完成一行通知

## 檔名 convention

| 類型 | Pattern |
|---|---|
| brainstorm 設計稿 (spec) | `docs/superpowers/specs/{YYYY-MM-DD}-{topic}-design.md` |
| implementation plan | `docs/superpowers/plans/{YYYY-MM-DD}-{topic}-implementation.md` |
| QA fix log | `docs/superpowers/plans/{YYYY-MM-DD}-qa-fixes-log.md` |
| canonical feature doc | `docs/product/{feature}.md` |
