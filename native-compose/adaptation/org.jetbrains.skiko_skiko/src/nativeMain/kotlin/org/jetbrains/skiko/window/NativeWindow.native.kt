package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.internal.sdl.*

object NativeWindow {
    private var running = false

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
        render: (canvas: Canvas, width: Int, height: Int, nanoTime: Long) -> Unit,
    ) = memScoped {
        check(!running) { "A NativeWindow event loop is already running" }
        running = true
        try {
            check(SDL_Init(SDL_INIT_VIDEO)) { SDL_GetError()?.toKString() ?: "SDL_Init failed" }
            try { runWindow(options, onOpen, onEvent, onClose, render) }
            finally { SDL_Quit() }
        }
        finally { running = false }
    }

    private fun runWindow(options: NativeWindowOptions = NativeWindowOptions(), onOpen: (NativeWindowScope) -> Unit, onEvent: (NativeWindowEvent) -> Unit, onClose: () -> Unit, render: (Canvas, Int, Int, Long) -> Unit) = memScoped {
        val software = options.backend == NativeWindowBackend.SOFTWARE
        var flags = SDL_WINDOW_RESIZABLE or SDL_WINDOW_HIGH_PIXEL_DENSITY
        if (!software) flags = flags or gpuWindowFlags()
        if (options.transparent) flags = flags or SDL_WINDOW_TRANSPARENT
        if (options.undecorated) flags = flags or SDL_WINDOW_BORDERLESS
        val window = checkNotNull(RachelSDL_CreateWindow(options.title, options.width, options.height, flags)) {
            SDL_GetError()?.toKString() ?: "SDL_CreateWindow failed"
        }
        try {
            var nanoTime = 0L
            val draw: (Canvas, Int, Int) -> Unit = { canvas, width, height -> render(canvas, width, height, nanoTime) }
            val renderer = if (software) RasterWindowRenderer(window, draw) else createGpuRenderer(window, options.transparent, draw)
            try {
                val wakeEventType = SDL_RegisterEvents(1)
                check(wakeEventType != UInt.MAX_VALUE) { "Unable to register the Native window wake event" }
                val scope = SdlWindowScope(window, wakeEventType)
                try {
                    onOpen(scope)
                    val events = SdlWindowEvents(window, wakeEventType, onEvent)
                    println("Renderer: ${renderer.description}; statically linked SDL3")
                    val event = alloc<SDL_Event>()
                    val width = alloc<IntVar>()
                    val height = alloc<IntVar>()
                    var dirty = true
                    while (!scope.shouldClose) {
                        if (SDL_WaitEventTimeout(event.ptr, if (dirty) 0 else 250)) {
                            do {
                                events.dispatch(event)
                                dirty = true
                            } while (SDL_PollEvent(event.ptr))
                        }
                        scope.rethrowCallbackFailure()
                        if (scope.shouldClose || !dirty) continue
                        check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, height.ptr))
                        check(width.value in 0 .. 16384 && height.value in 0 .. 16384 && width.value.toLong() * height.value <= 67108864)
                        val frameStarted = SDL_GetTicks()
                        nanoTime = SDL_GetTicksNS().toLong()

                        dirty = false
                        if (width.value > 0 && height.value > 0) renderer.render(width.value, height.value)
                        val elapsed = SDL_GetTicks() - frameStarted
                        if (elapsed < 16uL) SDL_Delay((16uL - elapsed).toUInt())
                    }
                }
                finally {
                    try { onClose() }
                    finally { scope.dispose() }
                }
            }
            finally { renderer.close() }
        }
        finally { RachelSDL_DestroyWindow(window) }
    }
}
