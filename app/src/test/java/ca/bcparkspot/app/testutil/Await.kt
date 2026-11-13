package ca.bcparkspot.app.testutil

import org.robolectric.shadows.ShadowLooper

object AwaitTiming {
    const val TIMEOUT_MS = 5_000L
    const val POLL_MS = 20L
}

/** Runs the main looper until [condition] holds, failing after [timeoutMs]. For work that hops to Room's executor. */
fun awaitUntil(timeoutMs: Long = AwaitTiming.TIMEOUT_MS, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "Condition not met within $timeoutMs ms" }
        ShadowLooper.idleMainLooper()
        Thread.sleep(AwaitTiming.POLL_MS)
    }
}
