# Coverage

> 怎麼量、看哪、v8 跨 process 的盲點、目標門檻。當前實測數字在 [status.md](./status.md)。

## 怎麼量

```bash
# 整套（含 integration，慢）
npx vitest run --coverage

# 只 unit + lib（快）
npx vitest run functions/lib functions/api tests/unit --coverage
```

Report 落在 `coverage/` 底下：
- `coverage/index.html` — 互動報表
- `coverage/coverage-summary.json` — script 解析用
- terminal — text-summary（top-level pct）

`vitest.config.ts` 設定範例：

```ts
coverage: {
  provider: 'v8',
  reporter: ['text-summary', 'json-summary', 'html'],
  include: ['functions/**/*.ts'],     // 視專案調整
  exclude: ['**/*.test.ts', '**/*.d.ts'],
  reportOnFailure: true,
}
```

## v8 跨 process 限制

API handler dir（`functions/api/**` 之類）在 coverage 報告永遠是 **0%**，**這是預期行為**：

- Integration test 跑 `fetch('http://localhost:8788/api/...')` 打 wrangler dev
- wrangler dev 是**獨立 Node process**（不同 PID）
- v8 instrument 只追蹤 vitest 自己這個 process 的 modules
- 跨 process 的 endpoint executions 不會回流到 instrument

不要試圖修這個 — 修法都是侵入性的（reverse-proxy、in-process Pages emulator），ROI 太低。改靠：

- **Integration test 數量** — 所有 API endpoint 都有專屬 test file
- **E2E user-facing 路徑** — Playwright spec 蓋 critical journey

## 抽出的 frontend module 不在 include

抽出的 `app/lib/` module 在 `tests/unit/*.test.ts` 真的有跑，但 coverage `include: ['functions/**/*.ts']` 不包 `app/`，所以報告看不到。

要納入：

```ts
coverage: {
  include: ['functions/**/*.ts', 'app/lib/**/*.js'],
  // ...
}
```

**先別動** — `app/lib/` 是抽出的純函式，每個都有 dedicated test file 配對，看 test 數就知道有跑。Coverage 報告值在「找沒測的 line」，不在「重複確認測過的 line」。

## 目標門檻

| 範圍 | Lines | Functions | Branches |
|---|---:|---:|---:|
| 純 helper lib root | ≥ 90% | ≥ 95% | ≥ 85% |
| Vendor adapter sub-dir | ≥ 80% | ≥ 90% | ≥ 85% |
| 資料正規化 sub-dir | ≥ 90% | ≥ 95% | ≥ 85% |
| API handler dir | — | — | — |（v8 看不到）|
| 抽出的 frontend module | — | — | — |（不在 include）|

當前實測見 [status.md](./status.md#覆蓋率)。

## 為什麼不追 100%

- v8 line coverage **不等於** behavior coverage — 100% line 可以漏 N+1 branch
- 重要的是 **branches + critical paths** 被打到
- 防呆 fallback 路徑、log-only catch、type narrowing 死路 — 蓋 100% 是 noise

## 看具體缺什麼

```bash
# Open HTML report
open coverage/index.html

# 找 < 80% lib files
cat coverage/coverage-summary.json | python3 -c "
import json, sys
data = json.load(sys.stdin)
for k, v in data.items():
  if 'functions/lib/' in k and v['lines']['pct'] < 80:
    print(f'{v[\"lines\"][\"pct\"]:5.1f}%  {k.split(\"functions/\")[-1]}')
"
```

## 不投資項目

| 項目 | 為什麼不追 |
|---|---|
| 100% 整體 line coverage | 修飾性 noise，dead-letter branch 蓋了沒用 |
| 100% UI styling | 視覺 review 比 visual regression 便宜 |
| Frontend HTML 內 inline DOM 操作 | 真實 bug 風險在純邏輯，不在 DOM ops — 抽 module → unit 即可 |
| Cloudflare Pages handler 本身 | v8 跨 process 限制；integration 蓋就好 |
