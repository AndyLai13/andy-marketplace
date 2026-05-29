---
name: android-testing
description: Write or update Android tests for the edu-vbos-finch :app module. Use when adding unit tests, ViewModel tests, Repository tests, Robolectric tests, Flow/StateFlow tests, WorkManager tests, instrumented UI tests, or any verification work that touches `app/src/test/` or `app/src/androidTest/`. Encodes the project's actual testing conventions (MockK, Turbine, Robolectric, JUnit 4) and points at canonical examples in-repo.
---

# Android testing — edu-vbos-finch conventions

Reality of this codebase as of 2026-04-29: 29 unit/Robolectric tests in `app/src/test/`, 12 instrumented tests in `app/src/androidTest/`, no Compose UI tests yet. `:system-stubs` is stub-only (no tests).

## Tooling — what's actually in use

- **Mocking**: **MockK 1.13.12** (`io.mockk:mockk`). Do **not** introduce Mockito.
- **Test framework**: **JUnit 4** (`@Test`, `@Before`, `@After`, `Assert.*`). Not JUnit 5.
- **Android-context tests**: **Robolectric 4.13**, locked to `@Config(sdk = [30])`.
- **Flow / StateFlow**: **Turbine 1.2.0** (`app.cash.turbine.test`).
- **Coroutines**: `kotlinx-coroutines-test`, **`StandardTestDispatcher`** + `Dispatchers.setMain`/`resetMain`. Not `UnconfinedTestDispatcher`.
- **HTTP**: **MockWebServer** (`com.squareup.okhttp3:mockwebserver`) for Retrofit/OkHttp paths.
- **WorkManager**: `androidx.work:work-testing` with `TestListenableWorkerBuilder`.
- **Coverage**: **Kover 0.8.3, IntelliJ engine** (JaCoCo breaks under Robolectric on AGP 8.x). Run `./gradlew :app:koverHtmlReportDebug`. Report at `app/build/reports/kover/htmlDebug/index.html`. `androidTest` and generated classes (`BuildConfig`, `R`, databinding, `*$serializer`) are excluded — see `app/build.gradle.kts:119-138`.
- **Required gradle config**: `testOptions { unitTests { isIncludeAndroidResources = true } }` (Robolectric needs Android resources). Already set at `app/build.gradle.kts:60-64`.

## Decision tree — pick the test type first

```
What are you testing?
├── ViewModel / UseCase / Repository logic     → JUnit + MockK + Robolectric
├── Pure domain logic (no Android types)       → JUnit + MockK (no Robolectric)
├── WorkManager Worker                         → JUnit + Robolectric + TestListenableWorkerBuilder
├── HTTP client / Retrofit                     → JUnit + MockWebServer (+ Robolectric if Context-bound)
├── Flow / StateFlow assertions                → Turbine inside any of the above
├── Compose UI                                 → Instrumented + ComposeTestRule (no examples yet — bootstrap from AndroidX docs)
└── WindowManager overlays / system services   → Test the state machine in JUnit; verify visuals manually
```

When in doubt, check whether the class touches `Context`, `ContentResolver`, `AccountManager`, `WorkManager`, or any `android.*` runtime — if yes, you need Robolectric.

## Canonical templates

### Unit / ViewModel test (Robolectric + MockK + Turbine + coroutines)

Pattern: `app/src/test/java/com/viewsonic/finch/appauth/login/QrCodeLoginViewModelCharacterizationTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FooViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockDataSource: ApiDataSource
    private lateinit var viewModel: FooViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDataSource = mockk(relaxed = true)
        viewModel = FooViewModel(mockDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loads data on init`() = runTest(testDispatcher) {
        coEvery { mockDataSource.fetch() } returns Result.Success(payload)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(UiState.Loaded(payload), awaitItem())
        }
    }
}
```

### Robolectric + AccountManager / system service

Pattern: `app/src/test/java/com/viewsonic/finch/appauth/accountmanager/LoginAuthenticatorTest.kt`

Use `RuntimeEnvironment.getApplication()` for the `Application` context. Mock callback responses with `mockk<AccountAuthenticatorResponse>(relaxed = true)`.

### WorkManager Worker test

Pattern: `app/src/test/java/com/viewsonic/finch/appauth/accountmanager/RefreshTokenWorkerTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FooWorkerTest {
    private val context by lazy { RuntimeEnvironment.getApplication() }

    @Test
    fun `worker succeeds`() = runTest {
        val worker = TestListenableWorkerBuilder<FooWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
```

### Instrumented test (no Compose)

Pattern: `app/src/androidTest/java/com/viewsonic/finch/ui/components/vssnackbar/VSSnackBarControllerInstrumentedTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class FooInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // teardown that must run on main
        }
    }
}
```

## MockK idioms used in this codebase

- `mockk<Type>(relaxed = true)` for collaborators where most calls return defaults.
- `every { x.foo() } returns y` / `coEvery { x.foo() } returns y` for suspend functions.
- `mockkStatic(SomeClass::class)` for static / top-level functions; pair with `unmockkAll()` in `@After`.
- `mockkObject(SomeObject)` for Kotlin singletons.
- `verify { x.bar() }` / `coVerify { ... }` for suspend.
- `every { x.foo() } just Runs` for `Unit`-returning functions.

## Architecture rule (testing-relevant)

Domain layer depends only on interfaces; concrete implementations are injected via constructors. If mocks keep multiplying for one class under test, that's a DI smell — refactor the boundary rather than adding more mocks.

## Pitfalls

- **Don't import Mockito.** The project standardizes on MockK. A new test that pulls in `org.mockito` should be rewritten before merge.
- **Don't switch to JaCoCo.** Documented incompatibility with Robolectric `SandboxClassLoader` on AGP 8.x. The Kover IntelliJ engine choice is intentional — see comment in `app/build.gradle.kts`.
- **`@Config(sdk = [30])` is mandatory** for Robolectric tests in this project; the default SDK that Robolectric picks may not match the project's targetSdk and will surface as cryptic class-loader errors.
- **`Dispatchers.setMain` requires `Dispatchers.resetMain` in `@After`** or it leaks across tests.
- **`isIncludeAndroidResources = true`** is already enabled in app gradle — don't disable it; Robolectric needs it for resource access.
- **No Compose tests exist yet.** When adding the first one, also add the `androidx.compose.ui:ui-test-junit4` dependency to `androidTestImplementation`.
- **CI does not currently run tests.** `.github/workflows/ticket_build.yml` only builds. Don't assume CI caught a test regression — run `./gradlew test` locally before pushing test changes.

## Run commands

```bash
./gradlew :app:testDebugUnitTest                  # unit + Robolectric tests
./gradlew :app:connectedDebugAndroidTest          # instrumented (needs emulator)
./gradlew :app:test --tests "*.FooViewModelTest"  # single test class
./gradlew :app:koverHtmlReportDebug               # coverage HTML
```

## Discovery tips for unfamiliar areas

- Browse `app/src/test/java/com/viewsonic/finch/appauth/` — the most thoroughly tested package; a good source of patterns for repositories, workers, and authenticators.
- Search `@RunWith(RobolectricTestRunner::class)` for all Robolectric examples.
- Search `Turbine` or `.test {` for Flow/StateFlow patterns.
- Search `MockWebServer` for HTTP test patterns.
