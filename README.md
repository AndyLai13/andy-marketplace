# andy-marketplace

Andy Lai 的 Claude Code plugin marketplace。

## 安裝

```bash
# 本機路徑
/plugin marketplace add ~/andy-marketplace

# 或之後 push 到 GitHub 後
# /plugin marketplace add https://github.com/<user>/andy-marketplace
```

## Plugins

| Plugin | 用途 | 安裝 |
|---|---|---|
| `android-testing` | Android `:app` 測試慣例（MockK / Turbine / Robolectric / JUnit 4） | `/plugin install android-testing@andy-marketplace` |
| `dual-loop-flow` | 多 PR feature 縱切 + 雙循環 (outside-in) TDD 規劃 | `/plugin install dual-loop-flow@andy-marketplace` |
| `gwt` | Given-When-Then Android test scaffold | `/plugin install gwt@andy-marketplace` |

裝完之後 skill 變成 namespaced，例如 `/dual-loop-flow:dual-loop-flow`。

## 更新

修改本機檔案後在 Claude Code：

```
/plugin marketplace update
```
