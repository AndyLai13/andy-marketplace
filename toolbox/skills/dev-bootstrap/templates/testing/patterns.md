# Test Patterns

> 寫測試時抄這份。每個 pattern 都是踩坑後沉澱的 — 不抄會撞同個牆。
> Doctrine 在 [shape.md](./shape.md)；本檔只放可重用的 code 樣板。

## 索引

| 想做什麼 | 看哪一節 |
|---|---|
| Mock 整個 SupabaseClient 寫 unit | [Mocked SupabaseClient template](#mocked-supabaseclient-template) |
| 測 vendor adapter + AES envelope | [Vendor adapter + AES envelope mock](#vendor-adapter--aes-envelope-mock) |
| 測 `@supabase/ssr` cookie callbacks | [vi.doMock module-level deps](#vidomock-module-level-deps) |
| 測 RPC 並發 race | [Concurrent RPC race](#concurrent-rpc-race) |
| 把前端 inline JS 抽出來 unit-test | [Frontend module 抽取流程](#frontend-module-抽取流程) |
| 寫 integration test | [Cloud Supabase integration](#cloud-supabase-integration) |
| 寫 E2E（Playwright）| [E2E data-testid 慣例](#e2e-data-testid-慣例) |

---

## Mocked SupabaseClient template

Unit-test 純函式或 handler 時 mock 整個 SupabaseClient。直接 import 真正的 source code，回傳值用 vi 控。

```ts
import { describe, it, expect, vi } from 'vitest'
import type { SupabaseClient } from '@supabase/supabase-js'

function makeClient(opts: {
  rpc?: { data?: unknown; error?: { code?: string; message?: string } | null }
  rows?: Array<Record<string, unknown>>
}): SupabaseClient {
  const rpc = vi.fn().mockResolvedValue(opts.rpc ?? { data: [], error: null })
  const from = vi.fn().mockImplementation((_table: string) => {
    const builder: Record<string, unknown> = {}
    builder.select = vi.fn().mockReturnThis()
    builder.insert = vi.fn().mockImplementation(() => ({
      select: vi.fn().mockReturnThis(),
      single: vi.fn().mockResolvedValue({ data: { id: 'new-id' }, error: null }),
    }))
    builder.update = vi.fn().mockImplementation(() => ({
      eq: vi.fn().mockResolvedValue({ data: null, error: null }),
    }))
    builder.eq = vi.fn().mockImplementation(() => {
      if ((builder.eq as ReturnType<typeof vi.fn>).mock.calls.length >= 2) {
        return Promise.resolve({ data: opts.rows ?? [], error: null })
      }
      return builder
    })
    builder.maybeSingle = vi.fn().mockResolvedValue({ data: opts.rows?.[0] ?? null, error: null })
    builder.single = vi.fn().mockResolvedValue({ data: opts.rows?.[0] ?? null, error: null })
    return builder
  })
  return { rpc, from } as unknown as SupabaseClient
}
```

**警示**：

- builder 是 mutable — 兩條 chain 共用同個 stub 會互相影響。每次回新 builder 比較安全。
- `eq` 計次 >= 2 是 hack，反映 `from(t).select().eq(a, b).eq(c, d)` 是兩次 `eq` 後才解析 rows。
- `update` 鏈尾要 `.eq()` 解析 promise。

---

## Vendor adapter + AES envelope mock

ECPay V3 envelope 之類的 vendor adapter — 不要 mock 掉 decrypt 函式，反而是直接 encrypt 一個假的 inner JSON 當回應。

```ts
import { aesEncryptForEcpayV3 } from './crypto-utils'

const credentials = { merchantId: 'XXX', hashKey: 'YYY', hashIv: 'ZZZ' }

async function envelopeOf(inner: Record<string, unknown>): Promise<Response> {
  const encrypted = await aesEncryptForEcpayV3(
    JSON.stringify(inner), credentials.hashKey, credentials.hashIv,
  )
  return new Response(JSON.stringify({
    TransCode: 1, TransMsg: '', Data: encrypted,
  }), { status: 200 })
}

it('issueInvoice returns IssueInvoiceResult on RtnCode=1', async () => {
  const v = new ECPayVendor(credentials, 'sandbox')
  vi.spyOn(global, 'fetch').mockResolvedValue(await envelopeOf({
    RtnCode: 1, RtnMsg: 'OK', InvoiceNo: 'AB12345678',
  }))
  const r = await v.issueInvoice(/* ... */)
  expect(r.invoice_number).toBe('AB12345678')
})
```

理由：vendor 的真實 bug 通常在 envelope/padding/signature 演算法 — mock 掉就測不到。讓 encrypt/decrypt 真跑，只 mock 網路層。

---

## vi.doMock module-level deps

`@supabase/ssr` 之類 module-level export 需要在 import 前 stub 掉時用 `vi.doMock`（不是 `vi.mock`）。

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest'

describe('createServerClient with custom cookies', () => {
  beforeEach(() => vi.resetModules())

  it('passes cookie handlers to @supabase/ssr', async () => {
    const createServerClient = vi.fn()
    vi.doMock('@supabase/ssr', () => ({ createServerClient }))

    const { userClient } = await import('./supabase')   // ← AFTER doMock
    userClient(/* req */, /* env */, new Headers())

    expect(createServerClient).toHaveBeenCalledWith(
      expect.any(String), expect.any(String),
      expect.objectContaining({
        cookies: expect.objectContaining({ get: expect.any(Function) }),
      })
    )
  })
})
```

`vi.mock` 是 hoisted — 用它無法在不同 test 換 mock；`vi.doMock` + dynamic `import()` 才能。

---

## Concurrent RPC race

測 RPC 並發競合（譬如 `confirm_online_payment` 被 webhook + polling 同時呼叫）：

```ts
it('confirm_online_payment is idempotent under Promise.all race', async () => {
  const ctx = await seedPaymentLink({ /* ... */ })

  const calls = Array.from({ length: 5 }, () =>
    serviceClient.rpc('confirm_online_payment', {
      p_transaction_id: ctx.txnId,
      p_vendor_txn_id: 'VENDOR123',
      p_vendor_payload: { mock: true },
    })
  )
  const results = await Promise.all(calls)

  // 全部 ok，但只一個有效寫入
  const ok = results.filter(r => !r.error)
  expect(ok.length).toBe(5)

  // 用 service-role 直接確認 DB
  const { data: txn } = await serviceClient.from('transactions')
    .select('status, paid_at').eq('id', ctx.txnId).single()
  expect(txn?.status).toBe('paid')

  // 收據只開一張
  const { data: receipts } = await serviceClient.from('receipts')
    .select('id').eq('transaction_id', ctx.txnId)
  expect(receipts?.length).toBe(1)
})
```

---

## Frontend module 抽取流程

把 HTML inline JS 抽出來 unit-test 的標準流程：

1. **挑純函式** — DOM-less、side-effect-free、跟 API 沒直接耦合的 helper / template builder
2. **新增 `app/lib/<name>-helpers.js`** — 用 `export function …` 標準 ESM
3. **寫 `tests/unit/<name>-helpers.test.ts`** — `@ts-expect-error` import 純 JS module
4. **HTML 內加 `<script type="module">` 把 module 掛到 `window.__<name>`** — defer load 不影響 inline script
5. **inline 改用 delegator**：`const fn = (x) => window.__<name>.fn(x)`（fn body 推遲到 call time，避免 race）
6. **跑 unit test 全綠後 commit**

範例 commit message：

```
refactor(<page>): extract <N> inline helpers to app/lib/<name>-helpers.js + <M> unit tests

<page>.html <BEFORE> → <AFTER> LOC. Inline logic now delegates to unit-tested module:
  helperA — purpose
  helperB — purpose
  ...
Unit suite: <X> → <Y> tests (+<M>).
```

---

## Cloud Supabase integration

每個 integration test 該長這樣：

```ts
import { beforeAll, afterAll, describe, it, expect } from 'vitest'
import { seedStudio, cleanup } from './helpers/seed'
import { authedFetch } from './helpers/authed-fetch'

describe('POST /api/studio/students', () => {
  let ctx: Awaited<ReturnType<typeof seedStudio>>

  beforeAll(async () => { ctx = await seedStudio() })
  afterAll(async () => { await cleanup(ctx) })

  it('creates a student linked to the studio', async () => {
    const res = await authedFetch('/api/studio/students', {
      method: 'POST', coach: ctx.owner,
      body: JSON.stringify({ name: '王小明', primary_coach_id: ctx.owner.id }),
    })
    expect(res.status).toBe(201)
    const body = await res.json()
    expect(body.data?.student?.studio_id).toBe(ctx.studio.id)
  })
})
```

**關鍵紀律**：

- `beforeAll` seed，`afterAll` cleanup — 不要散在 `beforeEach`
- 不 mock RLS，直接打真 endpoint
- assertions 用 service-role 確認（hidden state），不要相信 endpoint return shape 而已

---

## E2E data-testid 慣例

`data-testid` 命名規則：

| 元素類型 | 命名 |
|---|---|
| Row / list item | `<entity>-row` 例 `student-row`、`receipt-row` |
| Button | `<verb>-<entity>-btn` 例 `void-invoice-btn`、`row-invoice-btn` |
| Drawer / dialog | `<entity>-panel` 例 `invoice-detail-panel`、`student-edit-panel` |
| Form field | `f-<field>` 例 `f-name`、`f-carrier-value` |

Playwright spec 用 `page.getByTestId('student-row').filter({ hasText: '王小明' })` 而非 CSS selector。

---

## 相關

- [shape.md](./shape.md) — 為什麼這樣分 tier
- [multi-tenant-safety.md](./multi-tenant-safety.md) — RLS cross-leak（critical pattern）
- [ci.md](./ci.md) — fileParallelism 為何要 false
