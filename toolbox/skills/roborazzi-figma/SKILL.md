---
name: roborazzi-figma
description: Use when an Android screen (Jetpack Compose OR View/XML) built from a Figma design doesn't visually match the spec (wrong spacing/padding/font size/corner radius), when calibrating a screen against a Figma reference, or when you want a repeatable render→diff→fix loop instead of eyeballing. Drives Roborazzi screenshot diffing in the edu-vbos-finch :app module, using a Figma export as the calibration reference and your own render as the long-term regression golden. This is the execution mechanism for dual-loop-flow's Rule 3b "match Figma" verify step.
---

# roborazzi-figma — close the Figma↔Android loop with Roborazzi

Agents build UI from Figma **once, blind** — produce code, never look at the result. This skill gives them eyes: render the screen (Compose **or** View/XML), diff it against the Figma reference, read the diff image, fix, repeat until it converges. Then lock the render as a regression golden.

**REQUIRED BACKGROUND:** test tooling (Robolectric/MockK/coroutines versions, `isIncludeAndroidResources`) lives in **toolbox:android-testing** — read it for anything not about screenshots.

**RELATIONSHIP TO dual-loop-flow:** `toolbox:dual-loop-flow` Rule 3b (Element Inventory Gate) *mandates* a per-element "match Figma pixel-for-pixel" verify step but only at planning level — it never says how. **This skill is that "how."** When a dual-loop tasks.md emits `- [ ] Bind <element> geometry to Figma node <id>`, this loop is what closes it.

## The one trap that breaks everyone

**A Figma export will NEVER pixel-match an Android render.** Font hinting, antialiasing, subpixel rounding, and Robolectric's native renderer differ from Figma's renderer (true for both Compose and View/XML). A raw pixel diff against a Figma export is always > 0. So:

> Do **not** use the Figma export as a tight pass/fail golden. Use it as a *calibration reference* — a target to converge toward by eye/vision, with antialiasing absorbed by `SimpleImageComparator(vShift/hShift)` — then switch to your own render as the golden for CI.

## Hard rule: numbers come from Figma data, NOT from the screenshot

The screenshot is for the **final visual diff only**. Never read spacing, padding, font size, color, or radius off a screenshot by eye — those are estimates and they drift.

> Pull every numeric/token value from `mcp__claude_ai_Figma__get_variable_defs` and `mcp__claude_ai_Figma__get_metadata` (exact tokens, dp, sp, hex). The screenshot/export is used ONLY as the visual diff target at the end of each round.

When a delta shows up in the `_compare.png`, fix it by looking up the **real value in the Figma metadata/variables**, not by nudging until it "looks right." Nudging-to-fit is how you converge to the wrong number.

## Two phases, never mix them

| Phase | Golden = | `changeThreshold` | Purpose | Human in loop? |
|-------|----------|-------------------|---------|----------------|
| **Calibration** | Figma export (at render's px size) | loose, ~`0.05F` | get diff images to converge toward Figma | only final "ok" |
| **Guard** | your own render | tight, ~`0.001F` | block future UI regressions in CI | only when design changes |

## Setup (once per module)

Roborazzi plugin + deps — align versions with the module's AGP/Compose:

```kotlin
// build.gradle.kts (:app)
plugins { id("io.github.takahirom.roborazzi") version "1.32.0" }

dependencies {
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.32.0")
    // Compose screens only — View/XML screens don't need this:
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.0")
    testImplementation("androidx.compose.ui:ui-test-junit4:<compose_version>")
}
// testOptions { unitTests { isIncludeAndroidResources = true } } already set — see android-testing
```

## The screenshot test (one per screen)

The threshold MUST be wired through a `RoborazziRule` — a bare `captureRoboImage(...)` ignores it. Flip the **one** marked value between phases (`0.05F` calibration → `0.001F` guard). The `RoborazziRule` + annotations + threshold are **identical for Compose and View/XML** — only the "render the screen + capture" lines differ (shown below).

### Compose variant

```kotlin
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)                       // native render ≈ real device; non-negotiable
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)      // pin device/density/locale → kills dp↔px drift
class LoginScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val roborazzi = RoborazziRule(                   // ← threshold lives HERE, not in captureRoboImage
        options = RoborazziRule.Options(
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.05F,                  // ★ THE knob: 0.05F calibration → 0.001F guard
                    imageComparator = SimpleImageComparator(
                        maxDistance = 0.007F,
                        vShift = 2, hShift = 2,               // absorb Figma↔render antialiasing
                    ),
                ),
            ),
        ),
    )

    @Test fun loginScreen() {
        compose.setContent {
            AppTheme {                                         // MUST wrap real theme — Typography/colorScheme drive the look
                LoginScreen(state = previewState)              // realistic state; this data is yours to craft (not automatable)
            }
        }
        // filename MUST equal the Figma export filename placed at the same path (see loop step 0)
        compose.onRoot().captureRoboImage("src/test/screenshots/login_screen.png")
    }
}
```

Pin the device qualifier, wrap `AppTheme`, and keep the capture filename identical to the Figma export filename — mismatches here are the top cause of false fails.

### View / XML variant

Same `@RunWith` / `@GraphicsMode` / `@Config` / `RoborazziRule` block as above (omitted for brevity). Only the body of the `@Test` changes — inflate/build the view, bind state, measure+layout, then capture. No `roborazzi-compose` / `createComposeRule` needed.

```kotlin
@Test fun loginScreen() {
    val context = RuntimeEnvironment.getApplication()
    // 1) inflate (or use ViewBinding: LoginScreenBinding.inflate(LayoutInflater.from(context)).root)
    val view = LayoutInflater.from(context).inflate(R.layout.login_screen, null).apply {
        // 2) bind realistic state — yours to craft (not automatable)
        findViewById<TextView>(R.id.title).text = "Sign in"
    }
    // 3) measure + layout at the device width, else the view has 0×0 and capture is blank
    val widthPx = context.resources.displayMetrics.widthPixels
    view.measure(
        View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),  // wrap height
    )
    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    // filename MUST equal the Figma export filename (see loop step 0)
    view.captureRoboImage("src/test/screenshots/login_screen.png")
}
```

The View gotcha that wastes an hour: **forgetting `measure()`+`layout()`** → the view is 0×0 → a blank/garbage capture. Theme comes from the test Activity/`@Config` (or set a theme on a `ContextThemeWrapper` before inflate) — the View equivalent of "wrap `AppTheme`."

Activity-hosted screens (the View counterpart of a full Compose screen): launch with Robolectric and capture the decor/content root instead of inflating manually:

```kotlin
val activity = Robolectric.buildActivity(LoginActivity::class.java).setup().get()
activity.window.decorView.captureRoboImage("src/test/screenshots/login_screen.png")
// or, via Espresso:  onView(isRoot()).captureRoboImage("src/test/screenshots/login_screen.png")
```

## The repeatable loop (this is the skill)

Run per screen via the helper script. The mechanical run + locate-diff step is scripted; the *judgement* steps are yours.

```
CALIBRATION
 0. Get the render's REAL pixel size first (don't guess from "1080×2400"):
      ./scripts/roborazzi-compare.sh "*LoginScreenScreenshotTest" :app record   # writes the golden once
      → read the produced PNG (src/test/screenshots/login_screen.png) dimensions.
    Export the Figma frame to EXACTLY those dimensions. Keep an untouched copy in figma-ref/
    (the golden path gets overwritten in GUARD — don't lose the reference), then copy it onto the golden:
      cp figma-ref/login_screen.png src/test/screenshots/login_screen.png
 1. Set changeThreshold = 0.05F in the RoborazziRule.
 2. ./scripts/roborazzi-compare.sh "*LoginScreenScreenshotTest" :app compare
 3. Read RESULT from the script:
      RESULT=pass  → converged enough → go to GUARD.
      RESULT=diff  → Read each path in DIFF_IMAGES (the *_compare.png, 3-up: reference|render|diff).
 4. VISION STEP: Read the *_compare.png and name concrete deltas
      ("button content-padding too wide", "title 2sp too small", "card radius too sharp").
 5. Fix the UI code (Compose or View/XML) for those deltas. Handle Material defaults explicitly
      (Button contentPadding, TextField min-height 56dp, ripple/elevation) — they override your values.
 6. Goto 2. Stop after 5 rounds OR when deltas stop shrinking → ask the human (don't fiddle forever).

GUARD  (only after a human confirms "close enough")
 7. ./scripts/roborazzi-compare.sh "*LoginScreenScreenshotTest" :app record   # lock YOUR render as golden
 8. Set changeThreshold = 0.001F. git add app/src/test/screenshots/*.png        # golden in git → UI diffs show in review
 9. CI runs: ./scripts/roborazzi-compare.sh "*LoginScreenScreenshotTest" :app verify
      Red + diff image only when someone changes the UI. Design changed on purpose? re-run step 7.
```

```
figma-ref/ copy ─cp→ golden ─compare→ 3-up diff (build/outputs/roborazzi) ─Read→ vision names deltas
      ↑                                                                              │
      └──────────────── fix UI code (mind Material defaults) ←──────────────────────┘
                            ↓ human: "ok"
              record own render = golden → CI verify (tight threshold)
```

## Where Roborazzi writes things (don't hunt blindly)

- **3-up diff images**: `build/outputs/roborazzi/*_compare.png` ← what the vision step Reads.
- **JSON diff results**: `build/test-results/roborazzi/`.
- **HTML report**: `build/reports/roborazzi/index.html`.

## Convergence & stop rules

- **Cap at 5 rounds per screen.** If passing isn't reached and the diff isn't shrinking, stop and surface the remaining deltas — usually a missing token, an unavailable font, or a Material default fighting you.
- **Never silently raise the threshold to force a pass.** Loose threshold is calibration-only; a guard golden that only passes at `0.05F` is not a golden. The script logs the threshold/mode it ran with.
- **One screen at a time.** `--tests "*<Screen>ScreenshotTest"` keeps the diff set small enough to reason about.

## Common mistakes

| Symptom | Cause | Fix |
|---------|-------|-----|
| `--tests` ignored / "unknown option" | filter passed to `compareRoborazziDebug` wrapper | script runs `testDebugUnitTest --tests … -Proborazzi.test.<mode>=true` — the standard test task honors `--tests` |
| Threshold seems to do nothing | set on `captureRoboImage`, not the rule | wire `changeThreshold` in `RoborazziRule.Options` |
| Always fails on size mismatch first | Figma exported at wrong px (e.g. full 1080×2400 vs content bounds) | `record` once, read produced PNG size, export Figma to THAT size |
| Always red even when identical | Figma export used as tight golden | calibration phase only; switch to own render for guard |
| Pixel-correct values, still looks wrong | Material component defaults override (padding/min-size/ripple) | set `contentPadding`/`TextFieldDefaults` explicitly, or drop to `Box`/`BasicTextField` |
| Render drifts run-to-run | no device qualifier / `LEGACY` graphics | `@Config(qualifiers = Pixel7)` + `GraphicsMode.NATIVE` |
| Colors/fonts off across the board | theme not applied | Compose: wrap `AppTheme` in `setContent`. View: theme via test Activity/`@Config` or `ContextThemeWrapper` before inflate |
| View capture is blank / 0×0 | `measure()`+`layout()` skipped before capture | measure at device width + `UNSPECIFIED` height, then `layout(...)` |
| Figma reference lost after going to guard | `record` overwrote the golden path | keep the untouched copy in `figma-ref/` |
| Loop never ends | agent fiddling past convergence | enforce 5-round cap → ask human |

## What stays manual (by design)

Crafting realistic `previewState` per screen, and the final "this is close enough" judgement. Everything else — running, locating diffs, naming deltas, fixing, re-running — the loop automates.

## Lineage / why this is different

The web/React ecosystem has mature open-source versions of the *agent self-verify loop* (e.g. `figma-to-code-claude-pipeline`, various Figma-to-code skills): Playwright screenshot → vision/human-judged compare → iterate. None of them use a screenshot-test golden, and none split a calibration pass from a CI regression golden. The Android ecosystem (Roborazzi, Paparazzi, ComposablePreviewScanner) has the golden-image infra but no Figma/agent loop. **This skill grafts the two:** the web loop's "extract values, screenshot only for final diff" discipline (above) onto Roborazzi's `_compare.png` pixel-diff, plus the calibration→guard split that neither side has.
