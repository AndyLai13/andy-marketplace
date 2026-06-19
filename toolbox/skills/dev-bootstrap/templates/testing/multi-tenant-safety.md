# Multi-tenant Safety

> **critical** pattern：每個 table 都需要「Tenant A JWT 不能讀 Tenant B row」測試。
> RLS 是 multi-tenant 隔離的 first-class 機制，這層 test 是最後一道防線。
>
> 如果本專案不是 multi-tenant，可以刪掉這個檔。

## 為什麼必跑

- Supabase RLS policy 是 SQL 寫的；改動 policy 沒測 = 永遠不知道規則漏沒漏
- App layer 不重複做 tenant filtering — 全靠 RLS 自動套
- 一個 missing policy = 整個 table 對所有 tenant 開放讀寫
- E2E 蓋不到（一次只一個 JWT 登入）；unit 蓋不到（mock 掉 RLS 等於沒 RLS）— **只有 integration 蓋得了**

## Pattern

每個 critical table 寫 `<table>-cross-leak.test.ts`：

```ts
import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { seedTenantWithOwner, seedRow, cleanup, testServiceClient } from './helpers/seed'
import { authedFetch } from './helpers/authed-fetch'

describe('<TABLE> — cross-tenant RLS', () => {
  let ctxA: Awaited<ReturnType<typeof seedTenantWithOwner>>
  let ctxB: Awaited<ReturnType<typeof seedTenantWithOwner>>

  beforeAll(async () => {
    ctxA = await seedTenantWithOwner()
    ctxB = await seedTenantWithOwner()
    await seedRow(ctxA, { name: 'A-row' })
  })

  afterAll(async () => {
    await cleanup(ctxA)
    await cleanup(ctxB)
  })

  it('Tenant B owner cannot LIST Tenant A rows', async () => {
    const res = await authedFetch('/api/.../list', {
      method: 'GET',
      coach: ctxB.owner,   // ← Tenant B JWT
    })
    const body = await res.json()
    expect(body.data?.items ?? []).toHaveLength(0)
  })

  it('Tenant B owner cannot READ Tenant A row by direct id', async () => {
    const rowA = await seedRow(ctxA, { name: 'A-direct' })
    const res = await authedFetch(`/api/.../${rowA.id}`, {
      method: 'GET',
      coach: ctxB.owner,
    })
    // RLS makes the row invisible → 404 (not 403)
    expect(res.status).toBe(404)
  })

  it('Tenant B owner cannot UPDATE Tenant A row', async () => {
    const rowA = await seedRow(ctxA, { name: 'A-update' })
    const res = await authedFetch(`/api/.../${rowA.id}`, {
      method: 'PATCH',
      coach: ctxB.owner,
      body: JSON.stringify({ name: 'pwned' }),
    })
    expect(res.status).toBe(404)   // RLS hides → 404
    // verify with service-role that the row didn't change
    const after = await testServiceClient()
      .from('<table>').select('name').eq('id', rowA.id).single()
    expect(after.data?.name).toBe('A-update')
  })

  it('Tenant B owner cannot DELETE Tenant A row', async () => {
    // 同樣 shape
  })
})
```

## Critical tables 必跑名單

每改 schema 都要確認這些 table 的 cross-leak test 跟得上。視專案填入：

- (例) `studios` / `tenants`
- (例) `users` / `members`
- (例) `transactions` / `orders`
- (例) `events`（audit log）
- ...

## 404 vs 403

RLS 隱藏 row 後，PostgREST 回 `0 rows`，handler 通常翻成 **404**（不是 403）。原因：

- 403 = 「這資源存在但你沒權限」
- 404 = 「找不到」

RLS 讓「找不到」變真，因為 query 看不到 row，自然視為不存在。**不要 assert 403 — 會錯**。

## 不 mock RLS

```ts
// ❌ 別這樣：unit 測 mock 掉 SupabaseClient = mock 掉 RLS
const mockClient = {
  from: () => ({ select: () => Promise.resolve({ data: studentRow }) })
}

// ✅ 應這樣：integration 真打 cloud Supabase
const client = userClient(req, env, headers)   // 帶 JWT 的 user client
const { data } = await client.from('students').select('*')
```

Unit-level 測 RPC business logic 可以 mock，但 RLS 必須 integration。

## service-role 用法

`testServiceClient()` bypass RLS — 用法限定：

- **setup**：seed data 進 DB
- **assertion**：用 service-role 確認 row 真的沒被改（如上面 UPDATE 測試）

**不要用 service-role 跑 endpoint test** — 那是測「我有後門」，不是測 RLS。

## 相關

- [shape.md](./shape.md) — 為何 RLS 測試只能 integration
- [patterns.md](./patterns.md) — `seedTenantWithOwner` + `authedFetch` 樣板
