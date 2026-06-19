# CI Policy

> 什麼跑在哪、什麼門檻擋 merge、什麼留給手動 smoke。

## 規劃中的 CI 政策

當寫 GitHub Actions 時：

### 每次 push 跑

| Tier | 跑法 | 為什麼 |
|---|---|---|
| Unit | `npx vitest run functions/lib functions/api tests/unit` | 快（~5s）+ catch 純函式 regression |
| Integration | `npx vitest run tests/integration` | 慢（~5min）但抓 RPC/RLS bug |

`fileParallelism: false` 強制 — 跨 file 並行會跟同個 cloud Supabase project deadlock。

### 只在 main / release PR 跑

| Tier | 為什麼留到 main |
|---|---|
| E2E（Playwright） | wrangler dev + playwright launch 大概 5 min；不每 PR 跑 |
| Coverage report 上傳 | 加重 CI 時間，main merge 後算就好 |

### 不跑

| 項目 | 原因 |
|---|---|
| `vitest --coverage` 在每 PR | v8 跨 process 限制；數字不準的時候沒意義 |
| Vendor sandbox 實際 round-trip | 手動 smoke |
| 跨瀏覽器（Firefox / Safari） | 只支援 chromium desktop（見 [shape.md](./shape.md#不投資-yagni)） |
| Visual regression | 不投資（同上）|

## fileParallelism warning

`vitest.config.ts`：

```ts
test: {
  fileParallelism: false,    // ← 必須是 false
  hookTimeout: 90000,
  testTimeout: 30000,
}
```

**為什麼**：

- Integration test 共用同一個 cloud Supabase project
- 每個 file 的 `beforeAll` / `afterAll` 都 seed / wipe 同名 studios / tenants
- 平行跑 → race → 被別 file 的 cleanup 提前 wipe → FK 違規 / RLS 撞名 / 隨機失敗

**不要為了快關掉**。慢 5 min 但綠，比快 1 min flaky 好。

## 路徑健康指標

當 CI 跑起來時 watch 這些：

| 指標 | 紅旗 |
|---|---|
| Test count 突然下降 | 有人 `.skip` 或 `.fixme`（可能該修但跳過）|
| 單一 file 跑 > 30s | `testTimeout: 30000` 會 fail；可能是 wrangler dev 沒起 |
| beforeAll 跑 > 90s | `hookTimeout: 90000` 會 fail；可能 cloud Supabase 慢/壞 |
| Integration 跑時間從 3min → 8min | 累積無 cleanup tenant 拖累所有 query；查 cleanup() 漏 |

## E2E 的特殊處理

- `playwright.config.ts` `retries: process.env.CI ? 2 : 1` — CI 機器抖動允許重試 2 次
- `webServer.reuseExistingServer: !process.env.CI` — 本地 reuse 已開的 dev server，CI 每次新開
- `SLOW_MO=500 npx playwright test --headed` 視覺 debug 用，不是 CI

## 待辦清單

當寫 CI 時要 setup 的東西：

1. **GitHub Actions workflow** — `.github/workflows/test.yml`
2. **Secrets 設定** — `SUPABASE_URL`、`SUPABASE_SERVICE_ROLE_KEY`、`ENCRYPTION_KEY` 等
3. **Pre-commit hook**（optional）— `husky` + `lint-staged` 跑 changed file 的 test

## 相關

- [shape.md](./shape.md) — 哪 tier 跑哪、哪不投資
- [patterns.md](./patterns.md) — Pattern 樣板
- [status.md](./status.md) — 當前測試健康度
