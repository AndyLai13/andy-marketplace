# andy-marketplace

Andy Lai 的 Claude Code plugin marketplace。

## 安裝

```bash
# 本機路徑
/plugin marketplace add ~/andy-marketplace

# 或之後 push 到 GitHub 後
# /plugin marketplace add https://github.com/<user>/andy-marketplace
```

## Plugin：`toolbox`

整包一個 plugin，內含 4 個 skill：

```
/plugin install toolbox@andy-marketplace
```

| Skill | 用途 | 安裝後叫用 |
|---|---|---|
| `android-testing` | Android `:app` 測試慣例（MockK / Turbine / Robolectric / JUnit 4） | `/toolbox:android-testing` |
| `gwt` | 改寫 / 檢查 Acceptance Criteria 的 Given-When-Then 結構與具體度 | `/toolbox:gwt` |
| `dual-loop-flow` | 多 PR feature 縱切 + 雙循環 (outside-in) TDD 規劃 | `/toolbox:dual-loop-flow` |
| `cleanup-merged-branch` | feature branch 收尾：同步 main、確認已 merge 才刪 local branch | `/toolbox:cleanup-merged-branch` |

> `dual-loop-flow` 的 AC 前置步驟會引用 `/toolbox:gwt` —— 兩者同包，裝 `toolbox` 即同時具備。

## 更新

修改本機檔案後在 Claude Code：

```
/plugin marketplace update
```
