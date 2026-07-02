---
name: test-strategy
description: Use when designing or evaluating a project's test-tier strategy on any stack where critical behavior lives OUTSIDE unit-testable code — BaaS / serverless (logic in PL/pgSQL RPC + RLS), Android (behavior in real-device paths: animation / gesture / IPC / lifecycle), or Flutter (route / gate-provider + async-notifier realize timing that only emerges under a real ProviderScope + router + pump loop). Defines the meta-principle (when the pyramid under-serves you and you must fatten a non-unit "necessity tier"), three concrete shape instances (BaaS Pyramid-leaning Trophy 65/30/5; Android unit / instrumented / manual funnel; Flutter unit / device-free widget-flow bowl / thin device-smoke + manual cap), tier role boundaries, the non-investment list, and coverage philosophy. Pure doctrine — does not scaffold files (see dev-bootstrap) or write test code (see android-testing / android-test-funnel).
---

# Test Strategy Doctrine — choosing a tier shape when logic escapes the unit tier

The shape, roles, and explicit non-investment list for projects whose critical behavior
does **not** live in plain unit-testable code — it lives in the database (PostgreSQL RPC +
RLS on a BaaS stack), on the device (real animation / gesture / IPC on Android), or in the
realized route / provider graph (go_router redirect + async-notifier timing on Flutter).

## The meta-principle (read this first)

The classic test pyramid (70/20/10, unit-heavy) assumes your **business logic lives in
unit-testable code** — thick service / repository layers, framework-agnostic functions,
UI verifiable by component tests. **When that assumption breaks, the pyramid
under-serves you**: the critical behavior sits in a layer unit tests structurally cannot
reach, so you are *forced* to fatten a non-unit tier — a **"necessity bowl."**

Three different stacks hit the same wall:

| Stack | Where critical behavior escapes the unit tier | Forced-fat "necessity" tier |
|---|---|---|
| **BaaS / serverless** | PL/pgSQL RPC (`confirm_*`, `run_payouts`) + RLS multi-tenant isolation | **Integration** (real Postgres + edge runtime) |
| **Android** | Real-device paths — animation wall-clock, gesture / MotionEvent routing, IPC / screenshot, real lifecycle / looper async | **Instrumented** (real device, `connectedAndroidTest`) |
| **Flutter** | Realized route graph — `go_router` redirect, gate-provider gating, `AsyncNotifier` realize timing (only emerge under real `ProviderScope` + router + `pump`) | **Widget-flow** (`WidgetTester`, **device-free**) + a thin device smoke (`integration_test`) |

All three end up **leaning away from pure Pyramid** — a wide unit base, a non-unit
"necessity" tier the stack forces on you, a thin critical-only top. The *reason* differs
(logic-in-DB vs behavior-on-device vs behavior-in-realized-route-graph); the *shape
pressure* is identical. Pick your shape by asking one question: **"where does my critical
behavior live, and can a unit test reach it?"**

Note the **cost** of the necessity tier differs sharply: BaaS integration and Android
instrumented are *expensive* (real Postgres / real device), so you keep them lean; Flutter's
widget-flow is *device-free* (Dart VM), so it's as cheap as unit — invest in it generously.

This skill = a **universal core** (coverage philosophy, non-investment list, doc split)
plus **three concrete shape instances**. Use the instance that matches your stack; borrow
the core for any stack.

## When to use

✅ Designing tier strategy for a project on any stack above (or a similar one —
   Vercel + Postgres RLS, Pages + Edge Functions, an Android `:app`, a Flutter client)
✅ Justifying tier ratios when a stakeholder asks "why not 70/20/10?"
✅ Deciding whether a given test belongs in unit, the necessity bowl, or the cap
✅ Writing a project's `docs/product/testing/shape.md` — use this as conceptual source,
   fill in project-specific examples

## When NOT to use

❌ A stack whose business logic genuinely lives in thick, unit-testable code layers
   → pure Pyramid 70/20/10 fits; this skill's "fatten a bowl" pressure doesn't apply
❌ All-in-on-integration philosophy (Testing Trophy 30/63/6) — this skill argues
   against it: the unit base stays wide on both stacks
❌ For *how* to write each tier's tests — that's the project's own `patterns.md`, the
   `android-testing` skill (Android stack conventions), or `android-test-funnel`
   (Android per-feature AC → tier triage)

---

# Shape instance A — BaaS / serverless (Pyramid-leaning Trophy 65/30/5)

For projects whose core business logic lives in PostgreSQL RPC + RLS, runs on edge
functions, and hosts a fair amount of frontend pure-function logic.

```
        ▲
       ███     E2E          ~5%   thin — critical journey only (Playwright)
      █████   Integration  ~30%   bowl — API + RPC + RLS (real Postgres)
    █████████  Unit         ~65%  base — pure helpers + adapters (Vitest)
```

**Not pure Pyramid 70/20/10**, **not pure Trophy 30/63/6**. Targets:
- **Unit ≥ 50%** (base wide)
- **Integration ≈ 30%** (one of two heavy tiers — BaaS necessity)
- **E2E ≤ 5%** (thin top — critical journeys only)

## Why not pure Pyramid (BaaS)

Pure Pyramid assumes thick service / repository layers, business logic in code, and UI
testable via unit-style component tests. **BaaS does not.** Forcing Pyramid means:

1. **Mocking SQL RPC** — core business rules (`confirm_*`, `complete_*`, `run_payouts`,
   etc.) live in PL/pgSQL. Unit-mocking the SupabaseClient tests your mock, not the rule.
2. **Mocking RLS** — multi-tenant isolation lives in RLS policies. A mocked client never
   violates RLS; bugs never surface.
3. **Over-splitting vendor adapters** — real vendor bugs (ECPay V5 empty-value signing,
   NewebPay dual padding, status-code drift) live in adapter internals. Unit covers them;
   live HTTP needs an integration smoke.

## Why not pure Trophy (BaaS)

Pure Trophy assumes the unit tier doesn't matter. **Frontend purity does:**

1. **Inline frontend JS has real bug surface** — vendor credential downgrade matrices,
   period math edge cases, row-shape transforms. These are Pyramid-shaped pure functions;
   Trophy thinking misses them. Extracting frontend → `lib/` modules → unit tests is the
   highest-ROI investment for this stack.
2. **Vendor adapters have many boundary values** — testConnection paths, payload shape
   edges, cap/clamp logic. Integration round-trips to a sandbox are expensive;
   unit-mocking the vendor internals is faster and more precise.

## Tier roles (BaaS)

| Tier | Covers | Tooling | Speed | When |
|---|---|---|---|---|
| **Unit** | Pure functions, vendor-adapter internals, Zod schemas, extracted DOM-less frontend modules, Supabase callbacks | Vitest (mock SupabaseClient + global.fetch) | 50–500 ms/test | Every push |
| **Integration** | API endpoint round-trip, RPC atomicity, RLS cross-leak | Vitest + cloud Supabase + wrangler dev | 1–5 s/test, `fileParallelism: false` (shared cloud DB) | Every push |
| **E2E** | Onboarding wizard, critical-payment journey, cross-coach, monthly settlement, public student pages | Playwright + chromium desktop | 5–15 s/test, total ≤ 5 min | main / release PR |

## Tier boundaries (BaaS)

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

---

# Shape instance B — Android (unit-wide / instrumented-bowl / manual-cap)

For an Android `:app` where logic lives in ViewModels / repositories / pure helpers, but
real *interaction* behavior lives on the device. This is the shape the
**`android-test-funnel`** skill operationalizes per-feature (AC → unit / instrumented /
manual triage); this section is the *why* behind that funnel.

```
        ▲
       ███     Manual        ≤5%   cap  — pure visual vs Figma, perception only (human)
      █████   Instrumented  ~30%   bowl — real-device paths (animation/gesture/IPC/lifecycle)
    █████████  Unit          ~65%  base — ViewModels, repos, pure logic (JVM / Robolectric)
```

Targets:
- **Unit ≥ 65%** — Android has a rich unit surface: ViewModel + mocked repo, Flow /
  StateFlow logic, mappers, reducers, formatters, period / geometry math. Cheapest,
  CI-gating. Robolectric counts here (JVM, no device).
- **Instrumented ≈ 30%** — the necessity bowl. **NOT "rerun unit on a device."** Covers
  only what needs a real device: true animation (wall-clock interpolation), true gesture
  (MotionEvent routing), true IPC / screenshot, real lifecycle / looper async.
  `exported="false"` Activities pull via `ActivityScenario` in the app process.
- **Manual ≤ 5%** — the cap. Pure visual diff vs Figma, subjective feel, 真機畫面 content
  where instrument cost > manual value. Calibrated visual diff is delegated to
  `roborazzi-figma`, not done by eye repeatedly.

## Why not pure Pyramid (Android)

Forcing Pyramid means trying to unit-test the device behavior by mocking it — fake clock
for animation, synthetic events for gestures, stubbed IPC. **You then test your mock, not
the device.** The real regressions (animation never settles, gesture lands off-grid,
overlay timing drifts across process) only surface on a real device. This is the exact
mirror of BaaS "a mocked client never violates RLS."

## Why not pure Trophy (Android)

Pure Trophy assumes the unit tier doesn't matter. On Android the unit base is the
**highest-ROI tier**: ViewModel / repository / pure-helper logic is plentiful, cheap, and
CI-gating. Pushing it up into instrumented means slow, device-bound, flaky tests for
logic that never needed a device. Extracting logic out of Views / Activities into pure
functions → unit tests is the Android analog of BaaS "extract frontend → `lib/`."

## Tier roles (Android)

| Tier | Covers | Tooling | Speed | When |
|---|---|---|---|---|
| **Unit** | Pure functions, ViewModel (mocked repo), Flow / StateFlow logic, mappers, validation, geometry / period math | JUnit + MockK + Turbine + Robolectric (JVM) | 50–500 ms/test | Every push (`:app:testDebugUnitTest`) |
| **Instrumented** | Real animation, gesture / MotionEvent routing, IPC / screenshot, real lifecycle / looper async, non-exported Activity flows | Espresso / UiAutomator + `ActivityScenario` on a real device | 1–10 s/test, needs device | PR / pre-merge (`:app:connectedDebugAndroidTest`) |
| **Manual** | Pure visual vs Figma, subjective feel, rare device-state, perception-only checks | Human + `roborazzi-figma` for calibrated visual diff | n/a | Release / spot-check |

## Tier boundaries (Android)

| Path | Unit | Instrumented | Manual |
|---|:-:|:-:|:-:|
| Pure functions / formulas / clamp math | ✅ | ❌ | ❌ |
| ViewModel state logic (mocked repo) | ✅ | ❌ | ❌ |
| Flow / StateFlow emission | ✅ | ❌ | ❌ |
| Real animation reaching a state | ❌ | ✅ | ❌ |
| Gesture / MotionEvent routing | ❌ | ✅ | ❌ |
| Real IPC / screenshot content | ❌ | ✅ | ❌ |
| Lifecycle / looper async (postDelayed) | ❌ | ✅ | ❌ |
| Non-exported Activity flow | ❌ | ✅ | ⚠️ |
| Pure visual vs Figma | ❌ | ❌ | ✅ |
| Subjective feel / perception | ❌ | ❌ | ✅ |

✅ = must cover at this tier · ⚠️ = optional · ❌ = don't cover here

> **Key contrast with BaaS:** Android's **top tier is manual**, not Playwright E2E.
> Android's "automate the journey" pressure is absorbed by the *instrumented* bowl
> (a feature's real-device e2e), so the cap stays human-perception-only. The per-row
> `✅`-vs-`unit:` decision ("look at the path, not the value") is the operational
> mechanic — owned by `android-test-funnel`, not duplicated here.

---

# Shape instance C — Flutter (unit-wide / device-free widget-flow bowl / thin device-smoke + manual cap)

For a Flutter client app where business logic lives in Controllers / Notifiers /
repositories / pure helpers (all unit-testable on the Dart VM), but **route / redirect /
gate-provider gating and async-notifier realize timing** only emerge once the real
`ProviderScope` + `go_router` + `pump` loop runs. The distinctive Flutter fact: **its
necessity tier is device-FREE** — `WidgetTester` drives the full widget tree, navigation,
and async realization on the Dart VM, no simulator needed. So the interaction bowl is the
*cheapest* across all three instances; only launch + native-plugin behavior is truly
device-bound and stays a thin smoke.

```
        ▲
       ██      Manual / Device QA  ≤5%   cap   — real Supabase/OAuth, native-plugin feel, perception (human + TestFlight)
      ████    integration_test    ~10%  smoke — app boot + golden path + native plugins (real device/sim)
     ██████   Widget-flow         ~25%  bowl  — route/redirect/gate/async-realize/form flow (WidgetTester, device-FREE)
   █████████  Unit                ~60%  base  — Controllers(realized)/Notifiers/Repos/pure fns (flutter_test + mocktail)
```

Targets:
- **Unit ≥ 60%** — rich, cheap base: Controllers / Notifiers with mocked repos, repository
  parsing, pure fns (money / date / period / phone / error mapping), model round-trips.
- **Widget-flow ≈ 25%** — the necessity bowl, **device-free**. Drives real `go_router`
  redirects, gate-provider gating, `AsyncNotifier` realization, and multi-screen form /
  checkout flows via `WidgetTester` + `pumpAndSettle`. This is where the *route-flow* class
  of bug is caught — the class that leaves every unit test green.
- **integration_test ≈ 10%** — thin device smoke. Only what needs a real engine: app
  launch, one golden-path journey, native-plugin round-trips (share sheet, calendar,
  splash). **Not** a rerun of widget-flow on a device.
- **Manual ≤ 5%** — the cap. Real Supabase / Google OAuth, native-plugin *feel*,
  perception, TestFlight spot-checks.

## Why not pure Pyramid (Flutter)

Forcing Pyramid means unit-testing route / gate logic by mocking the router and provider
graph — you then verify your mock's wiring, not the real redirect + realize timing. The
canonical failure: **a coach mis-scoped into onboarding** — every unit test green, the API
returned the correct scope on curl, yet the realized `go_router` redirect + `AsyncNotifier`
sequence dead-ended the user. Only a widget-flow test (real `ProviderScope` + router +
`pump`) — or a device — surfaces it. This is the exact mirror of BaaS "a mocked client
never violates RLS" and Android "a stubbed clock never fails to settle."

## Why not pure Trophy (Flutter)

Pure Trophy assumes the unit tier doesn't matter. In Flutter the unit base is the
**highest-ROI tier**: Controllers with mocked repos, repositories, and pure helpers are
plentiful, millisecond-fast, and CI-gating. Pushing them up into widget or device tests
buys slow, flaky coverage for logic that never needed a widget tree. Extracting logic out
of `build()` methods into Controllers / pure functions is the Flutter analog of BaaS
"extract frontend → `lib/`" and Android "extract logic out of Views."

## Tier roles (Flutter)

| Tier | Covers | Tooling | Speed | When |
|---|---|---|---|---|
| **Unit** | Pure functions, Controllers / Notifiers (mocked repo), repository parsing, mappers, money / date / period math, validation | `flutter_test` + `mocktail` (Dart VM) | 50–500 ms/test | Every push (`flutter test`) |
| **Widget-flow** | `go_router` redirect, gate-provider gating, `AsyncNotifier` realize timing, multi-screen form / checkout flow | `flutter_test` `WidgetTester` + `pumpAndSettle` (Dart VM, **no device**) | 100 ms–2 s/test | Every push (`flutter test`) |
| **integration_test** | App launch, one golden-path journey, native-plugin round-trips (share / calendar / splash) | `integration_test` pkg + real device / simulator | 5–30 s/test | PR / pre-TestFlight (`flutter test integration_test -d <device>`) |
| **Manual** | Real Supabase / Google OAuth, native-plugin feel, perception, rare device state | Human + TestFlight | n/a | Release / device QA |

## Tier boundaries (Flutter)

| Path | Unit | Widget-flow | integration_test | Manual |
|---|:-:|:-:|:-:|:-:|
| Pure functions / money / date math | ✅ | ❌ | ❌ | ❌ |
| Controller / Notifier state (mocked repo) | ✅ | ❌ | ❌ | ❌ |
| Repository parsing / mappers | ✅ | ❌ | ❌ | ❌ |
| `go_router` redirect / gate-provider gating | ❌ | ✅ | ⚠️ (smoke) | ❌ |
| `AsyncNotifier` realize timing | ❌ | ✅ | ❌ | ❌ |
| Multi-screen form / checkout flow | ❌ | ✅ | ⚠️ (golden path) | ❌ |
| App launch / route boot | ❌ | ⚠️ | ✅ | ❌ |
| Native plugin (share / calendar / splash) | ❌ | ❌ | ✅ | ⚠️ |
| Real Supabase / Google OAuth journey | ❌ | ❌ | ❌ | ✅ |
| Perception / native-plugin feel | ❌ | ❌ | ❌ | ✅ |
| UI styling | ❌ | ❌ | ❌ | ❌ (non-invest) |

✅ = must cover at this tier · ⚠️ = optional · ❌ = don't cover here

> **Key contrast:** Flutter's necessity bowl is **device-free** — the cheapest interaction
> tier of all three instances. Android's interaction bowl is device-bound (keep it lean;
> push everything possible down to Robolectric); Flutter's is a Dart-VM `WidgetTester`
> (invest generously — it's as cheap as unit and catches the route-flow class of bug). So
> the "automate the journey" pressure is absorbed mostly by device-free widget-flow, with
> only a thin `integration_test` smoke for launch + native plugins above it, and a human
> manual cap on top.
>
> **Async-realize trap (Flutter-specific).** `AsyncValue.valueOrNull` returns the
> *retained previous value* during `AsyncLoading` / `AsyncError`. A widget-flow test that
> reads state before the provider realizes tests a stale value — the mirror of "test your
> mock, not the device." Guard: `await` the provider's `.future` (or `pumpAndSettle`) and
> assert `is AsyncData` before reading `.value`. Prefer pure-function tests of
> `gateFrom(AsyncValue)` where the gate logic can be lifted out of the widget.
>
> **SDD harness caveat.** `integration_test` (and any device run) backgrounds and hangs
> under a subagent-driven harness — the subagent blocks forever. Run device tests from the
> main loop; let subagents only *write* them.

---

# Universal core (all three stacks)

## Coverage philosophy

**Don't chase 100% line coverage** (universal). Line coverage ≠ behaviour coverage. What
matters is **branches + critical paths**. Defensive fallbacks, log-only catches,
type-narrowing dead-ends — covering them is noise on either stack.

Per-stack instrumentation caveats:

- **BaaS — v8 has a cross-process limit.** When integration tests `fetch` against
  `wrangler dev`, the endpoint runs in a **separate Node process**; v8 only sees the
  vitest process, so endpoint coverage shows 0%. **Do not fix this** — all fixes
  (reverse-proxy, in-process Pages emulator) are invasive and ROI-negative. Compensate
  with integration *count* (every endpoint gets a test file) + E2E on user-facing paths.
- **Android — instrumented coverage is not the unit coverage number.** JaCoCo on
  `connectedAndroidTest` is device-bound and noisy; don't gate CI on it. Gate on the
  **unit** coverage of the pure-logic dirs; treat instrumented as behaviour insurance
  measured by *case presence* (each real-device path has a dedicated instrumented test),
  not by a line %.
- **Flutter — unit + widget-flow share one lcov; integration_test does not.**
  `flutter test --coverage` instruments unit *and* widget-flow together (both Dart VM) into
  one `coverage/lcov.info` — gate on that for pure-logic dirs and Controllers.
  `integration_test` coverage is device-bound and noisy; don't gate on it — treat it as
  smoke *presence* (each critical journey / native-plugin path has a dedicated smoke),
  not a line %.

### Suggested thresholds (BaaS)

| Scope | Lines | Functions | Branches |
|---|---:|---:|---:|
| `functions/lib (root)` (or equivalent pure-helper dir) | ≥ 90% | ≥ 95% | ≥ 85% |
| `functions/lib/payment` (vendor adapters) | ≥ 80% | ≥ 90% | ≥ 85% |
| `functions/lib/migration` (data normalization) | ≥ 90% | ≥ 95% | ≥ 85% |
| `functions/api/**` | — | — | — (v8 cross-process) |
| Extracted frontend `lib/` modules | (manually verified; each module has a dedicated test file) |

### Suggested thresholds (Android)

| Scope | Lines | Functions | Branches |
|---|---:|---:|---:|
| Pure-logic / util packages | ≥ 90% | ≥ 95% | ≥ 85% |
| ViewModels (unit, mocked repo) | ≥ 80% | ≥ 90% | ≥ 80% |
| Adapters / mappers / formatters | ≥ 85% | ≥ 90% | ≥ 85% |
| Instrumented (`androidTest`) | — | — | — (device-bound; gate on case presence, not %) |

### Suggested thresholds (Flutter)

| Scope | Lines | Functions | Branches |
|---|---:|---:|---:|
| Pure-logic / core helpers (money / date / errors) | ≥ 90% | ≥ 95% | ≥ 85% |
| Controllers / Notifiers (unit, mocked repo) | ≥ 80% | ≥ 90% | ≥ 80% |
| Repositories / mappers | ≥ 85% | ≥ 90% | ≥ 85% |
| Widget-flow (`WidgetTester`) | — | — | — (gate on flow presence: each critical route / gate flow has a test) |
| integration_test (device) | — | — | — (device-bound; gate on smoke presence, not %) |

Adapt directory names to your project; the **principle** (purity tier ≥ 90% lines,
adapter tier ≥ 80%, device / infra tier excluded from the % gate) is the doctrine.

## Non-investment list (YAGNI)

**Universal:**

| Item | Why we don't invest |
|---|---|
| 100% overall line coverage | Decoration; dead-letter branches don't help |
| Visual-regression infrastructure | Visual review (or one calibrated golden) is cheaper than maintaining a diff farm |
| Cross-locale string testing | Single-locale projects don't need it |

**BaaS-specific:**

| Item | Why |
|---|---|
| Real vendor sandbox round-trips | Manual smoke is enough; sandbox endpoints often lack pre-flight APIs |
| Supabase Auth internal behaviour | Trust upstream; revisit only when Supabase changes |
| Cross-browser / mobile / iPad viewport | E2E runs chromium desktop only; rest is manual |
| ICS / calendar visual e2e | OS renderers; view-test ROI too low |
| Cloudflare Pages handler instrumentation | v8 cross-process limit (see Coverage Philosophy) |

**Android-specific:**

| Item | Why |
|---|---|
| Full visual-regression of every screen | One calibrated Figma golden per screen (`roborazzi-figma`) beats a full diff farm |
| Cross-device / API-level matrix in CI | Pick one reference device; the rest is manual spot-check |
| Instrumenting pure logic "to be safe" | Slow, device-bound, flaky; it belongs in unit |
| Subjective-feel automation | Perception (animation polish, haptics) is a human call — leave it in the manual cap |

**Flutter-specific:**

| Item | Why |
|---|---|
| Patrol / heavy UI-automation framework | `WidgetTester` widget-flow + a thin `integration_test` smoke covers the journey; Patrol is over-investment for a solo / pilot app |
| Golden / visual-regression file tests | Until the visual direction is locked, goldens fight every polish PR; use manual device QA vs design |
| Real Supabase + real Google OAuth E2E automation | Can't script the Google consent screen; device QA / TestFlight covers it |
| Cross-device / iPad-portrait / phone-viewport matrix | Test the primary viewport only; the rest is manual spot-check |
| Rerunning widget-flow on a device "for realism" | The Dart VM tree is faithful for routing / async; the device tier is for launch + native plugins only |

## Canonical vs Snapshot doc split

When a project actually writes this down, split into two doc genres:

| Genre | Content | Update mode | Files |
|---|---|---|---|
| **Canonical** | Shape target, tier roles, pattern code templates, coverage thresholds | Edit when the doctrine itself changes | `shape.md` / `patterns.md` / `coverage.md` / `ci.md` (+ stack-specific: BaaS `multi-tenant-safety.md`; Flutter an `async-traps.md` for the realize / fail-hold pitfalls) |
| **Snapshot** | Current test counts, ratios, per-domain distribution, gaps | Overwrite when stale — no historical batch files (git log is the audit trail) | `status.md` |

The canonical file *set* varies by stack: the BaaS instance adds `multi-tenant-safety.md`
(RLS cross-leak), the Flutter instance an `async-traps.md` (`AsyncValue` realize / gate
fail-hold / SDD device-run caveat); `shape.md` / `patterns.md` / `coverage.md` / `ci.md` /
`status.md` are common to all.

The `dev-bootstrap` skill (in this same toolbox) scaffolds the `docs/product/testing/`
folder with both genres.

## How to reference this skill from a project

The project's own `docs/product/testing/README.md` should cite this skill for the *why*
and keep project-specific instantiation (which RPCs / RLS policies, which vendor adapters,
which real-device paths, which route / gate flows, viewport / device scope) in its own
`shape.md` — picking the shape instance (A BaaS / B Android / C Flutter) that matches the
stack. Example link:

> 概念來源：`test-strategy` skill (andy-marketplace/toolbox)

## Related skills

- `android-testing` — Android stack test conventions (MockK / Turbine / Robolectric); the
  *how-to* under shape instance B's unit tier.
- `android-test-funnel` — Android per-feature AC → unit / instrumented / manual triage;
  the *orchestration* that applies shape instance B case by case.
- `dev-bootstrap` — scaffolds `docs/product/testing/` that cites this doctrine.
