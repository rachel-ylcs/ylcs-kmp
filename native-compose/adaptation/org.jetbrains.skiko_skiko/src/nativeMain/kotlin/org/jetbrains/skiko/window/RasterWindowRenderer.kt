package org.jetbrains.skiko.window

import org.jetbrains.skia.Canvas

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use

internal class RasterWindowRenderer(window: COpaquePointer, private val draw: (Canvas, Int, Int) -> Unit) : WindowRenderer {
    private val renderer = checkNotNull(RachelSDL_CreateRenderer(window, "software")) { SDL_GetError()?.toKString() ?: "SDL_CreateRenderer failed" }
    override val description = "Skia raster / SDL texture (CPU baseline)"

    override fun render(width: Int, height: Int): Boolean {
        Surface.makeRaster(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL)).use { surface ->
            draw(surface.canvas, width, height)
            Pixmap().use { pixels ->
                check(surface.peekPixels(pixels))
                val texture = checkNotNull(RachelSDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGBA32, SDL_TextureAccess.SDL_TEXTUREACCESS_STREAMING, width, height))
                try {
                    check(SDL_UpdateTexture(texture, null, interpretCPointer<ByteVar>(pixels.addr), pixels.rowBytes))
                    check(RachelSDL_RenderClear(renderer))
                    check(RachelSDL_RenderTexture(renderer, texture, null, null))
                    check(RachelSDL_RenderPresent(renderer))
                }
                finally { SDL_DestroyTexture(texture) }
            }
        }
        return true
    }

    override fun close() = RachelSDL_DestroyRenderer(renderer)
}