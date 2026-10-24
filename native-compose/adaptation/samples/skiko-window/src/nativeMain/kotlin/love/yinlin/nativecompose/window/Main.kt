@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.nativecompose.window

import kotlinx.cinterop.*
import love.yinlin.nativecompose.sdl.*
import org.jetbrains.skia.Canvas

/** SDL owns the event loop; Skiko owns the selected Ganesh backend. Use --software for the CPU baseline. */
fun main(args: Array<String>) = runNativeWindow(args, ::drawSkikoDemo)

actual fun runNativeWindow(args: Array<String>, draw: (Canvas, Int, Int) -> Unit) = memScoped {
    check(SDL_Init(SDL_INIT_VIDEO)) { SDL_GetError()?.toKString() ?: "SDL_Init failed" }
    try {
        val software = "--software" in args || "--cpu" in args
        val flags = SDL_WINDOW_RESIZABLE or SDL_WINDOW_HIGH_PIXEL_DENSITY or if (software) 0uL else gpuWindowFlags()
        val window = checkNotNull(RachelSDL_CreateWindow("Rachel Native / Skiko Ganesh", 800, 480, flags)) {
            SDL_GetError()?.toKString() ?: "SDL_CreateWindow failed"
        }
        try {
            val renderer = if (software) RasterWindowRenderer(window, draw) else createGpuRenderer(window, draw)
            try {
                println("Renderer: ${renderer.description}")
                val event = alloc<SDL_Event>()
                val width = alloc<IntVar>()
                val height = alloc<IntVar>()
                var running = true
                var dirty = true
                val smoke = "--smoke" in args
                val started = SDL_GetTicks()
                var initialWidth = 0
                var initialHeight = 0
                var rendered = 0
                while (running) {
                    check(!smoke || SDL_GetTicks() - started < 10000uL) { "SDL resize smoke test timed out" }
                    if (SDL_WaitEventTimeout(event.ptr, if (dirty) 0 else 250)) {
                        do {
                            if (event.type == SDL_EVENT_QUIT || event.type == SDL_EVENT_WINDOW_CLOSE_REQUESTED) running = false
                            dirty = true
                        } while (SDL_PollEvent(event.ptr))
                    }
                    if (!running || !dirty) continue
                    check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, height.ptr))
                    if (width.value <= 0 || height.value <= 0) {
                        dirty = false
                        continue
                    }
                    check(width.value in 1 .. 16384 && height.value in 1 .. 16384 && width.value.toLong() * height.value <= 67108864)
                    if (!renderer.render(width.value, height.value)) {
                        SDL_Delay(16u)
                        continue
                    }
                    dirty = false
                    rendered++
                    if (smoke && rendered == 1) {
                        initialWidth = width.value
                        initialHeight = height.value
                        check(RachelSDL_SetWindowSize(window, 960, 540))
                    }
                    else if (smoke && (width.value != initialWidth || height.value != initialHeight)) {
                        println("PASS: ${renderer.description}; SDL resize ${initialWidth}x$initialHeight -> ${width.value}x${height.value}")
                        running = false
                    }
                }
                println("SDL presentation complete: $rendered frame(s)")
            }
            finally { renderer.close() }
        }
        finally { RachelSDL_DestroyWindow(window) }
    }
    finally { SDL_Quit() }
}
