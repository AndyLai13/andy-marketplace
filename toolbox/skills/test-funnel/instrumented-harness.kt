/*
 * Instrumented 真機 e2e harness — 由 /toolbox:test-funnel 提供，供 layer (b) 自動化「原本手動」的驗證。
 * 萃取自 canonical 範例 edu-vbos-finch
 *   app/src/androidTest/.../zoom/ZoomActivityInstrumentedTest.kt（VB-579 / VB-882，IFP41 全綠）。
 *
 * 三個你會反覆用到的 pattern：
 *   1. ActivityScenario 拉「exported=false」的 Activity（adb shell uid 2000 無法 am start，
 *      在 app 自身 process uid 1000 內合法拉起）。
 *   2. 手勢注入（單指拖曳走真實 dispatch pipeline，Robolectric 測不到）。
 *   3. settle-gating（輪詢加逾時 + 收斂閘控，取代固定 sleep，避 flake）。
 *
 * 放置：app/src/androidTest/java/<pkg>/<Feature>InstrumentedTest.kt
 * 跑法：./gradlew :app:connectedDebugAndroidTest（需實機 / 模擬器；CI 不跑）
 */
package com.example.feature

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.hamcrest.Matchers.any
import org.junit.After
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class FeatureInstrumentedTest {

    private lateinit var scenario: ActivityScenario<TargetActivity>

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    // ── pattern 1：ActivityScenario 拉非-exported Activity ─────────────────────
    // launch 後一律用 onActivity{} 進入 main thread 讀/寫 view；跨執行緒的回傳值用
    // 單元素陣列外帶出來（onActivity 的 lambda 在另一條 thread 跑）。

    private fun launch() {
        scenario = ActivityScenario.launch(TargetActivity::class.java)
    }

    private fun click(id: Int) = scenario.onActivity { it.findViewById<View>(id).performClick() }

    private fun enabledNow(id: Int): Boolean {
        val out = BooleanArray(1)
        scenario.onActivity { out[0] = it.findViewById<View>(id).isEnabled }
        return out[0]
    }

    // ── pattern 3：settle-gating（避 flake 的核心）────────────────────────────
    // 非同步狀態（動畫、postDelayed、IPC 截圖）不可用固定 sleep 斷言——裝置負載高時
    // 固定 sleep 不夠。改為「輪詢到符合條件或逾時」。

    /** 輪詢某個浮點狀態趨近 target，或逾時。回傳最後讀到的值供斷言。 */
    private fun waitValueNear(read: () -> Float, target: Float, tol: Float = 0.06f, timeoutMs: Long = 5000): Float {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var v = read()
        while (abs(v - target) > tol && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(80)
            v = read()
        }
        return v
    }

    /** 輪詢某個布林狀態（如 isEnabled）達到期望，或逾時。 */
    private fun waitEnabled(id: Int, want: Boolean, timeoutMs: Long = 4000): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var e = enabledNow(id)
        while (e != want && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(80)
            e = enabledNow(id)
        }
        return e
    }

    /** 等非同步資源就緒（如 onResume 的 IPC 截圖把 bitmap 設上，手勢才有作用對象）。 */
    private fun waitReady(ready: () -> Boolean, timeoutMs: Long = 6000): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!ready() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        return ready()
    }

    /**
     * 收斂閘控：連續操作（連點）時，每擊都等前一個動畫「確實生效且兩次取樣不再變」才再點。
     * 為何不能固定 sleep 連點：mid-animation 的中間值可能被生產端邏輯判為非法 / off-grid 而跳回起點。
     * guard 上限防止無限迴圈。把 `step()` / `read()` / 收斂判定換成你的 feature 對應行為。
     */
    private fun repeatUntilSettled(read: () -> Float, target: Float, step: () -> Unit, maxClicks: Int = 8) {
        var guard = 0
        while (read() < target - 0.06f && guard++ < maxClicks) {
            val from = read()
            step()
            val deadline = SystemClock.uptimeMillis() + 2500
            var prev = Float.NaN
            var v = read()
            while (SystemClock.uptimeMillis() < deadline) {
                if (v > from + 0.06f && !prev.isNaN() && abs(v - prev) < 0.001f) break // 已生效且收斂
                prev = v
                SystemClock.sleep(100)
                v = read()
            }
        }
    }

    // ── pattern 2：手勢注入（單指拖曳，走真實觸控 dispatch）────────────────────
    // down → 數段 move 插值 → up，注入後 loopMainThreadUntilIdle，最後 recycle 所有 event。

    private fun swipeBy(dx: Float, dy: Float) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = any(View::class.java)
        override fun getDescription() = "swipe by ($dx, $dy)"
        override fun perform(uiController: UiController, view: View) {
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val sx = loc[0] + view.width / 2f
            val sy = loc[1] + view.height / 2f
            val downTime = SystemClock.uptimeMillis()
            val events = ArrayList<MotionEvent>()
            events += MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, sx, sy, 0)
            val steps = 12
            for (i in 1..steps) {
                val t = downTime + i * 12
                events += MotionEvent.obtain(downTime, t, MotionEvent.ACTION_MOVE, sx + dx * i / steps, sy + dy * i / steps, 0)
            }
            events += MotionEvent.obtain(downTime, downTime + (steps + 1) * 12L, MotionEvent.ACTION_UP, sx + dx, sy + dy, 0)
            uiController.injectMotionEventSequence(events)
            uiController.loopMainThreadUntilIdle()
            events.forEach { it.recycle() }
        }
    }

    // ── 範例 test：把上面拼起來 ───────────────────────────────────────────────
    // onView(withId(R.id.target)).perform(swipeBy(300f, 0f))
    // assertEquals(Lifecycle.State.DESTROYED, scenario.state)  // 等 lifecycle 也用 deadline 輪詢
}
