package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.internal.sdl.*

object NativeWindow {
    private var running = false
    private var session: SdlWindowSession? = null

    fun run(
        options: NativeWindowOptions = NativeWindowOptions(),
        onKey: (NativeWindowKey) -> Unit = {},
        render: (canvas: Canvas, width: Int, height: Int, deltaSeconds: Float) -> Unit,
    ) {
        var scope: NativeWindowScope? = null
        var previous = 0L
        open(options, onOpen = { scope = it }, onEvent = { event ->
            when (event) {
                NativeWindowEvent.CloseRequested -> scope?.close()
                is NativeWindowEvent.Key -> if (event.down && !event.repeat) {
                    when (event.scanCode) {
                        41 -> scope?.close()
                        44 -> onKey(NativeWindowKey.SPACE)
                        40 -> onKey(NativeWindowKey.ENTER)
                        80 -> onKey(NativeWindowKey.LEFT)
                        79 -> onKey(NativeWindowKey.RIGHT)
                        82 -> onKey(NativeWindowKey.UP)
                        81 -> onKey(NativeWindowKey.DOWN)
                    }
                }
                else -> Unit
            }
        }) { canvas, width, height, now ->
            val delta = if (previous == 0L) 0f else (now - previous).toFloat() / 1_000_000_000f
            previous = now
            render(canvas, width, height, delta)
        }
    }

    fun open(
        options: NativeWindowOptions = NativeWindowOptions(),
        onOpen: (NativeWindowScope) -> Unit = {},
        onEvent: (NativeWindowEvent) -> Unit = {},
        onClose: () -> Unit = {},
        onFrame: (Long) -> Unit = {},
        render: (canvas: Canvas, width: Int, height: Int, nanoTime: Long) -> Unit,
    ) {
        check(!running) { "A NativeWindow event loop is already running" }
        running = true
        try {
            check(RachelSDL_InitWindowHost()) { SDL_GetError()?.toKString() ?: "SDL_Init failed" }
            try {
                val host = SdlWindowSession()
                session = host
                try {
                    host.create(options, onOpen, onEvent, onClose, render)
                    host.run(onFrame)
                }
                finally {
                    try { host.close() }
                    finally { session = null }
                }
            }
            finally { SDL_Quit() }
        }
        finally { running = false }
    }

    /** Creates an independent window in the running event loop. Closing it does not close the main window. */
    fun openChild(
        options: NativeWindowOptions = NativeWindowOptions(),
        onOpen: (NativeWindowScope) -> Unit = {},
        onEvent: (NativeWindowEvent) -> Unit = {},
        onClose: () -> Unit = {},
        render: (canvas: Canvas, width: Int, height: Int, nanoTime: Long) -> Unit,
    ): NativeWindowScope = checkNotNull(session) { "Start the main NativeWindow before creating another window" }.create(options, onOpen, onEvent, onClose, render)
}
