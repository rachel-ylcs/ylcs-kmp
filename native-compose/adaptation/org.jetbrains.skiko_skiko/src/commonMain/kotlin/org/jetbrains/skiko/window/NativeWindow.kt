package org.jetbrains.skiko.window

import org.jetbrains.skia.Canvas

/**
 * Desktop Native window host supplied by this Skiko port.
 *
 * Run on the main thread. This call owns the window/event loop and blocks until
 * close or Escape; callbacks run on the same thread. The frame canvas is borrowed
 * and must not be retained or closed. Callback failures propagate after native
 * renderer/window resources are released. One event loop may run at a time.
 */
expect object NativeWindow {
    fun run(
        options: NativeWindowOptions = NativeWindowOptions(),
        onKey: (NativeWindowKey) -> Unit = {},
        render: (canvas: Canvas, width: Int, height: Int, deltaSeconds: Float) -> Unit,
    )
}