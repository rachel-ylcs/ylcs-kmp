@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package love.yinlin.nativecompose.window

import kotlinx.cinterop.*
import love.yinlin.nativecompose.sdl.*
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use

/** Skia raster -> SDL texture. This sample does not claim to validate Ganesh GPU backends. */
fun main(args: Array<String>) = memScoped {
    check(SDL_Init(SDL_INIT_VIDEO)) { SDL_GetError()?.toKString() ?: "SDL_Init failed" }
    try {
        val window = checkNotNull(RachelSDL_CreateWindow("Rachel Native / Skiko core", 800, 480, SDL_WINDOW_RESIZABLE or SDL_WINDOW_HIGH_PIXEL_DENSITY)) {
            SDL_GetError()?.toKString() ?: "SDL_CreateWindow failed"
        }
        try {
            val renderer = checkNotNull(RachelSDL_CreateRenderer(window, null)) { SDL_GetError()?.toKString() ?: "SDL_CreateRenderer failed" }
            try {
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
                    check(!smoke || SDL_GetTicks() - started < 5000uL) { "SDL resize smoke test timed out" }
                    // Wait while idle; one redraw after an expose/resize/input event.
                    if (SDL_WaitEventTimeout(event.ptr, if (dirty) 0 else 250)) {
                        do {
                            if (event.type == SDL_EVENT_QUIT || event.type == SDL_EVENT_WINDOW_CLOSE_REQUESTED) running = false
                            dirty = true
                        } while (SDL_PollEvent(event.ptr))
                    }
                    if (!running || !dirty) continue
                    check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, height.ptr))
                    if (width.value <= 0 || height.value <= 0) continue
                    // A bound protects allocation when a window system reports an invalid extent.
                    check(width.value <= 16384 && height.value <= 16384 && width.value.toLong() * height.value <= 67108864)
                    Surface.makeRaster(ImageInfo(width.value, height.value, ColorType.RGBA_8888, ColorAlphaType.PREMUL)).use { surface ->
                        val canvas = surface.canvas
                        canvas.clear(Color.WHITE)
                        Paint().use { paint ->
                            paint.color = Color.makeRGB(30, 105, 190)
                            canvas.drawRect(Rect.makeXYWH(24f, 24f, width.value - 48f, height.value / 2f), paint)
                            val fonts = FontMgr.default
                            val families: Array<String?> = ["Arial", "DejaVu Sans", "Liberation Sans", "Noto Sans"]
                            val face = fonts.matchFamiliesStyle(families, FontStyle.NORMAL)
                                ?: if (fonts.familiesCount > 0) fonts.matchFamilyStyle(fonts.getFamilyName(0), FontStyle.NORMAL) else null
                            face?.use { face ->
                                Font(face, 26f).use { font ->
                                    paint.color = Color.BLACK
                                    canvas.drawString("Skiko Native + SDL3 | resize / close", 24f, height.value - 40f, font, paint)
                                }
                            } ?: error("No system font available")
                        }
                        Pixmap().use { pixels ->
                            check(surface.peekPixels(pixels))
                            val texture = checkNotNull(RachelSDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGBA32, SDL_TextureAccess.SDL_TEXTUREACCESS_STREAMING, width.value, height.value))
                            try {
                                check(SDL_UpdateTexture(texture, null, interpretCPointer<ByteVar>(pixels.addr), pixels.rowBytes))
                                check(RachelSDL_RenderClear(renderer))
                                check(RachelSDL_RenderTexture(renderer, texture, null, null))
                                check(RachelSDL_RenderPresent(renderer))
                            }
                            finally { SDL_DestroyTexture(texture) }
                        }
                    }
                    dirty = false
                    rendered++
                    if (smoke && rendered == 1) {
                        initialWidth = width.value
                        initialHeight = height.value
                        check(RachelSDL_SetWindowSize(window, 960, 540))
                    }
                    else if (smoke && (width.value != initialWidth || height.value != initialHeight)) {
                        println("PASS: SDL resize ${initialWidth}x$initialHeight -> ${width.value}x${height.value}")
                        running = false
                    }
                }
                println("SDL raster presentation complete: $rendered frame(s)")
            }
            finally { RachelSDL_DestroyRenderer(renderer) }
        }
        finally { RachelSDL_DestroyWindow(window) }
    }
    finally { SDL_Quit() }
}
