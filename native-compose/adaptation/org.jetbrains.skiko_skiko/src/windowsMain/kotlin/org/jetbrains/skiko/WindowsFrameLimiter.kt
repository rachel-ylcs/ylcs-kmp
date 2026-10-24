@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlin.math.ceil
import platform.windows.Sleep

internal class WindowsFrameLimiter {
    private var nextFrameNanos = 0L

    fun awaitNextFrame(refreshRate: Float) {
        val safeRefreshRate = refreshRate.takeIf { it.isFinite() && it > 1f } ?: 60f
        val interval = (1_000_000_000.0 / safeRefreshRate).toLong()
        val now = currentNanoTime()
        if (nextFrameNanos > now) sleepNanos(nextFrameNanos - now)
        val afterWait = currentNanoTime()
        nextFrameNanos = maxOf(nextFrameNanos + interval, afterWait + interval)
    }

    private fun sleepNanos(duration: Long) {
        if (duration <= 0L) return
        val milliseconds = ceil(duration / 1_000_000.0).toLong().coerceAtLeast(1L)
        Sleep(milliseconds.coerceAtMost(UInt.MAX_VALUE.toLong()).toUInt())
    }
}
