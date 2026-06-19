# Test Shape & Strategy

> Canonical spec：本專案的 tier 比例目標、角色定義、不投資清單。
> Doctrine 出處：[`testing-pyramid-doctrine`](https://github.com/AndyLai13/andy-marketplace/tree/main/toolbox/skills/testing-pyramid-doctrine) skill。
> 當前實測數字在 [status.md](./status.md)。

## 目標形狀：Pyramid-leaning Trophy

```
        ▲
       ███     E2E         ~5%   薄 — critical journey only
      █████   Integration  ~30%  bowl — API + RPC + RLS
    █████████  Unit         ~65%  base — pure helpers + adapters
```

{{PROJECT_NAME}} **不採用** 70/20/10 嚴格 Pyramid，**也不採用** 30/63/6 純 Trophy。目標是 **65/30/5**：

- **Unit** ≥ 50%（base 寬）
- **Integration** ≈ 30%（仍是雙頂之一，BaaS 的必要）
- **E2E** ≤ 5%（薄頂，只蓋 critical journey）

完整論證見 [`testing-pyramid-doctrine` skill](https://github.com/AndyLai13/andy-marketplace/tree/main/toolbox/skills/testing-pyramid-doctrine#why-not-pure-pyramid-702010)。

## Tier 角色定義

| Tier | 蓋什麼 | 工具 | 速度 | 必跑 |
|---|---|---|---|---|
| **Unit** | 純函式、vendor adapter 內部、Zod schemas、前端抽出的 DOM-less module、Supabase callbacks | vitest（mock SupabaseClient + global.fetch） | 50-500ms/test | 每次 push |
| **Integration** | API endpoint round-trip、RPC atomicity、RLS cross-leak | vitest + cloud Supabase + wrangler dev | 1-5s/test，`fileParallelism: false` | 每次 push |
| **E2E** | 關鍵使用者旅程（onboarding 8-step、Path A/B 收款、月結等） | Playwright + chromium desktop | 5-15s/test，total ≤ 5 min | main / release PR |

## 三層的明確邊界

| 路徑 | Unit | Integration | E2E |
|---|:-:|:-:|:-:|
| 純函式 / 公式 | ✅ | ❌ | ❌ |
| Vendor 簽章 / 加密 | ✅ | ⚠️（smoke） | ❌ |
| Zod schema | ✅ | ❌ | ❌ |
| 前端純邏輯 module | ✅ | ❌ | ❌ |
| RPC 邏輯 | ❌ | ✅ | ❌ |
| RLS policy | ❌ | ✅ | ❌ |
| API 路由 + middleware | ❌ | ✅ | ✅ |
| 多 callback race | ❌ | ✅ Promise.all() | ❌ |
| 學員/教練 journey | ❌ | ⚠️（部分） | ✅ |
| UI styling | ❌ | ❌ | ❌（不投資） |

✅ = 必須蓋；⚠️ = 可以但不一定划算；❌ = 別在這層蓋。

## 不投資（YAGNI）

| 項目 | 為什麼不投資 |
|---|---|
| 100% UI styling | 視覺 review 比 visual regression test 便宜 |
| Vendor sandbox 實際 round-trip | 手動 smoke 即可 |
| Supabase Auth 內部行為 | trust the upstream — 改 supabase 才回頭看 |
| 字串國際化 | 視專案 locale 數量決定 |
| 跨瀏覽器 / iPad / mobile viewport | E2E 只跑 chromium desktop |
| ICS 視覺端到端 | OS 渲染，view-test ROI 太低 |
| Visual regression | 同上 |

**新增 / 移除「不投資」項目要 commit 動 doctrine。** 不要在 PR 內偷塞。

## Coverage 目標門檻

| 範圍 | 目標 lines | 目標 functions |
|---|---:|---:|
| 純 helper lib（`functions/lib`、`src/lib` 之類）| ≥ 90% | ≥ 95% |
| Vendor / 資料正規化 sub-dir | ≥ 80% | ≥ 90% |
| API handler dir | — | — |（v8 跨 process 看不到，靠 integration） |
| 抽出的 frontend module | (manually verified) | — |

詳見 [coverage.md](./coverage.md)。

## 相關

- [patterns.md](./patterns.md) — 怎麼寫各 tier 的測試（含 code 範本）
- [multi-tenant-safety.md](./multi-tenant-safety.md) — RLS cross-leak 必跑
- [ci.md](./ci.md) — 什麼跑在 push / 什麼跑在 main
- [status.md](./status.md) — 當前實測快照
