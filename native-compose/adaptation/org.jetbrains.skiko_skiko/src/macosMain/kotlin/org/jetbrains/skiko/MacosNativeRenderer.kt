package org.jetbrains.skiko

import org.jetbrains.skia.Canvas

internal interface MacosNativeRenderer {
    val description: String

    fun render(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ): Boolean

    fun close()
}
