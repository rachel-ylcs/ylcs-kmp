@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.internal.sdl.*

actual object NativeWindow {
    private var running = false

    actual fun run(options: NativeWindowOptions, onKey: (NativeWindowKey) -> Unit,
                   render: (Canvas, Int, Int, Float) -> Unit) = memScoped {
        check(!running) { "A NativeWindow event loop is already running" }
        running = true
        try {
            check(SDL_Init(SDL_INIT_VIDEO)) { SDL_GetError()?.toKString() ?: "SDL_Init failed" }
            try { runWindow(options, onKey, render) }
            finally { SDL_Quit() }
        }
        finally { running = false }
    }

    private fun runWindow(options: NativeWindowOptions, onKey: (NativeWindowKey) -> Unit,
                          render: (Canvas, Int, Int, Float) -> Unit) = memScoped {
        val software = options.backend == NativeWindowBackend.SOFTWARE
        val flags = SDL_WINDOW_RESIZABLE or SDL_WINDOW_HIGH_PIXEL_DENSITY or if (software) 0uL else gpuWindowFlags()
        val window = checkNotNull(RachelSDL_CreateWindow(options.title, options.width, options.height, flags)) {
            SDL_GetError()?.toKString() ?: "SDL_CreateWindow failed"
        }
        try {
            var delta = 0f
            val draw: (Canvas, Int, Int) -> Unit = { canvas, width, height -> render(canvas, width, height, delta) }
            val renderer = if (software) RasterWindowRenderer(window, draw) else createGpuRenderer(window, draw)
            try {
                println("Renderer: ${renderer.description}; statically linked SDL3")
                val event = alloc<SDL_Event>()
                val width = alloc<IntVar>()
                val height = alloc<IntVar>()
                val started = SDL_GetTicks()
                var previous = started
                var open = true
                var dirty = true
                var frames = 0
                var initialWidth = 0
                var initialHeight = 0
                while (open) {
                    check(!options.smoke || SDL_GetTicks() - started < 10000uL) { "NativeWindow smoke test timed out" }
                    if (SDL_WaitEventTimeout(event.ptr, if (dirty || options.continuous) 0 else 250)) {
                        do {
                            when (event.type) {
                                SDL_EVENT_QUIT, SDL_EVENT_WINDOW_CLOSE_REQUESTED -> open = false
                                SDL_EVENT_KEY_DOWN -> if (!event.key.repeat) {
                                    when (event.key.key) {
                                        SDLK_ESCAPE -> open = false
                                        SDLK_SPACE -> onKey(NativeWindowKey.SPACE)
                                        SDLK_RETURN -> onKey(NativeWindowKey.ENTER)
                                        SDLK_LEFT -> onKey(NativeWindowKey.LEFT)
                                        SDLK_RIGHT -> onKey(NativeWindowKey.RIGHT)
                                        SDLK_UP -> onKey(NativeWindowKey.UP)
                                        SDLK_DOWN -> onKey(NativeWindowKey.DOWN)
                                    }
                                }
                            }
                            dirty = true
                        } while (SDL_PollEvent(event.ptr))
                    }
                    if (!open || (!dirty && !options.continuous)) continue
                    check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, height.ptr))
                    check(width.value in 0 .. 16384 && height.value in 0 .. 16384 && width.value.toLong() * height.value <= 67108864)
                    val now = SDL_GetTicks()
                    delta = (now - previous).toFloat() / 1000f
                    previous = now
                    if (width.value > 0 && height.value > 0 && renderer.render(width.value, height.value)) {
                        frames++
                        dirty = false
                        if (options.smoke && frames == 1) {
                            initialWidth = width.value
                            initialHeight = height.value
                            check(RachelSDL_SetWindowSize(window, (options.width + 160).coerceAtMost(16384), (options.height + 60).coerceAtMost(16384)))
                        }
                        else if (options.smoke && frames >= (if (options.continuous) 12 else 2) &&
                            (width.value != initialWidth || height.value != initialHeight)) {
                            println("PASS: ${renderer.description}; static SDL; resize ${initialWidth}x$initialHeight -> ${width.value}x${height.value}; $frames frames")
                            open = false
                        }
                    }
                    if (options.continuous || dirty) SDL_Delay(16u)
                }
            }
            finally { renderer.close() }
        }
        finally { RachelSDL_DestroyWindow(window) }
    }
}
