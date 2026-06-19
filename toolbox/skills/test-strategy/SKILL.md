---
name: test-strategy
description: Use when designing or evaluating a project's test-tier strategy on a BaaS / serverless stack (Cloudflare Workers + Supabase, Pages + Edge Functions, Vercel + Postgres RLS, similar). Defines the Pyramid-leaning Trophy 65/30/5 shape, Unit/Integration/E2E role boundaries, what NOT to invest in, and coverage-target philosophy. Pure doctrine — does not scaffold files (see dev-bootstrap for that).
---

# Testing Pyramid Doctrine — Pyramid-leaning Trophy for BaaS

The shape, roles, and explicit non-investment list for projects whose core
business logic lives in PostgreSQL RPC + RLS, runs on edge functions, and
hosts a fair amount of frontend pure-function logic.

## When to use

✅ Designing tier strategy for a new BaaS project (Cloudflare Pages /
   Workers / Supabase / similar)
✅ Justifying tier ratios when a stakeholder asks "why not 70/20/10?"
✅ Deciding whether to mock SupabaseClient or hit a real Postgres in a
   given test
✅ Writing a project's `docs/product/testing/shape.md` — use this as
   conceptual source, fill in project-specific examples

## When NOT to use

❌ Heavily server-side stack with thick repository / service layers
   → pure Pyramid 70/20/10 fits better
❌ All-in-on-integration philosophy (Testing Trophy 30/63/6) — this skill
   intentionally argues against it for BaaS
❌ For *how* to write each tier's tests (Vitest mock patterns, RPC race
   harness, Playwright fixtures) — that's the project's own `patterns.md`

## The shape: Pyramid-leaning Trophy (65/30/5)

```
        ▲
       ███     E2E       ~5%   thin — critical journey only
      █████   Integration ~30%  bowl — API + RPC + RLS
    █████████  Unit       ~65%  base — pure helpers + adapters
```

**Not pure Pyramid 70/20/10**, **not pure Trophy 30/63/6**. Targets:
- **Unit ≥ 50%** (base wide)
- **Integration ≈ 30%** (still one of two heavy tiers — BaaS necessity)
- **E2E ≤ 5%** (thin top — critical journeys only)

## Why not pure Pyramid (70/20/10)

Pure Pyramid assumes thick service / repository layers, business logic in
code, and UI testable via unit-style component tests. **BaaS does not.** Forcing Pyramid means:

1. **Mocking SQL RPC** — core business rules (`confirm_*`, `complete_*`,
   `run_payouts`, etc.) live in PL/pgSQL. Unit-mocking the SupabaseClient
   tests your mock, not the real rule.
2. **Mocking RLS** — multi-tenant isolation lives in RLS policies. A mocked
   client never violates RLS; bugs never surface.
3. **Over-splitting vendor adapters** — real vendor bugs (ECPay V5 empty-
   value signing, NewebPay dual padding, status-code drift) live in the
   adapter internals. Unit covers them; live HTTP needs an integration
   smoke.

## Why not pure Trophy (30/63/6)

Pure Trophy assumes unit-tier doesn't matter. **Frontend purity does:**

1. **Inline frontend JS has real bug surface** — vendor credential
   downgrade matrices, period math edge cases, row-shape transforms.
   These are Pyramid-shaped pure functions; Trophy thinking misses them.
   Extracting frontend → `lib/` modules → unit tests is the highest-ROI
   investment for this stack.
2. **Vendor adapters have many boundary values** — testConnection paths,
   payload shape edges, cap/clamp logic. Integration round-trips to a
   sandbox are expensive; unit-mocking the vendor internals is faster and
   more precise.

## Tier roles

| Tier | Covers | Tooling | Speed | When |
|---|---|---|---|---|
| **Unit** | Pure functions, vendor-adapter internals, Zod schemas, extracted DOM-less frontend modules, Supabase callbacks | Vitest (mock SupabaseClient + global.fetch) | 50–500 ms/test | Every push |
| **Integration** | API endpoint round-trip, RPC atomicity, RLS cross-leak | Vitest + cloud Supabase + wrangler dev | 1–5 s/test, `fileParallelism: false` (shared cloud DB) | Every push |
| **E2E** | Onboarding wizard, critical-payment journey, cross-coach, monthly settlement, public student pages | Playwright + chromium desktop | 5–15 s/test, total ≤ 5 min | main / release PR |

## Explicit tier boundaries

What **only** belongs in integration, what **only** in unit, what spans both:

| Path | Unit | Integration | E2E |
|---|:-:|:-:|:-:|
| Pure functions / formulas | ✅ | ❌ | ❌ |
| Vendor signatures / crypto | ✅ | ⚠️ (smoke) | ❌ |
| Zod schemas | ✅ | ❌ | ❌ |
| Frontend pure-logic modules | ✅ | ❌ | ❌ |
| RPC logic | ❌ | ✅ | ❌ |
| RLS policies | ❌ | ✅ | ❌ |
| API routing + middleware | ❌ | ✅ | ✅ |
| Multi-callback race | ❌ | ✅ (Promise.all) | ❌ |
| End-user journeys | ❌ | ⚠️ (partial) | ✅ |
| UI styling | ❌ | ❌ | ❌ (non-invest) |

✅ = must cover at this tier · ⚠️ = optional · ❌ = don't cover here

## Non-investment list (YAGNI)

| Item | Why we don't invest |
|---|---|
| 100% UI styling tests | Visual review is cheaper than visual-regression infrastructure |
| Real vendor sandbox round-trips | Manual smoke is enough; sandbox endpoints often lack pre-flight APIs |
| Supabase Auth internal behaviour | Trust the upstream; revisit only when Supabase changes |
| Cross-locale string testing | Single-locale projects don't need it |
| Cross-browser / mobile / iPad viewport | E2E runs chromium desktop only; rest is manual |
| ICS / calendar visual e2e | OS renderers; view-test ROI too low |
| Visual regression | Same as above |
| 100% overall line coverage | Decoration; dead-letter branches don't help |
| Cloudflare Pages handler instrumentation | v8 cross-process limit (see Coverage Philosophy) |

## Coverage philosophy

**v8 has a cross-process limit.** When integration tests run
`fetch('http://localhost:8788/api/...')` against `wrangler dev`, the
endpoint executes in a **separate Node process**. v8 instrumentation only
sees the vitest process; the endpoint coverage shows as 0%.

**Do not fix this.** All fixes are invasive (reverse-proxy, in-process
Pages emulator) and ROI is negative. Compensate with:
- **Integration test count** — every API endpoint gets a dedicated
  integration test file
- **E2E user-facing paths** — Playwright covers critical journeys

**Don't chase 100% line coverage.** v8 line coverage ≠ behaviour coverage.
What matters is **branches + critical paths**. Defensive fallbacks,
log-only catches, type-narrowing dead-ends — covering them is noise.

### Suggested thresholds

| Scope | Lines | Functions | Branches |
|---|---:|---:|---:|
| `functions/lib (root)` (or equivalent pure-helper dir) | ≥ 90% | ≥ 95% | ≥ 85% |
| `functions/lib/payment` (vendor adapters) | ≥ 80% | ≥ 90% | ≥ 85% |
| `functions/lib/migration` (data normalization) | ≥ 90% | ≥ 95% | ≥ 85% |
| `functions/api/**` | — | — | — (v8 cross-process) |
| Extracted frontend `lib/` modules | (manually verified; each module has dedicated test file) |

Adapt the directory names to your project; the principle (purity tier ≥ 90% lines, vendor tier ≥ 80% lines, infra tier excluded) is the doctrine.

## Canonical vs Snapshot doc split

When a project actually writes this down, split into two doc genres:

| Genre | Content | Update mode | Files |
|---|---|---|---|
| **Canonical** | Shape target, tier roles, pattern code templates, coverage thresholds | Edit when the doctrine itself changes | `shape.md` / `patterns.md` / `coverage.md` / `multi-tenant-safety.md` / `ci.md` |
| **Snapshot** | Current test counts, ratios, per-domain distribution, gaps | Overwrite when stale — no historical batch files (git log is the audit trail) | `status.md` |

The `dev-bootstrap` skill (in this same toolbox) scaffolds the
`docs/product/testing/` folder with both genres.

## How to reference this skill from a project

The project's own `docs/product/testing/README.md` should cite this skill
for the *why* and keep project-specific instantiation (RPCs named, vendor
adapters used, viewport scope, etc.) in its own `shape.md`. Example link:

> 概念來源：`test-strategy` skill (andy-marketplace/toolbox)
