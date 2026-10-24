package org.jetbrains.skiko

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Pixmap
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceProps

internal class WindowsSoftwareRenderer(
    private val window: WindowsNativeWindow,
    override val renderApi: GraphicsApi,
    private val pixelGeometry: PixelGeometry,
) : WindowsLayerRenderer {
    init {
        require(renderApi == GraphicsApi.SOFTWARE_FAST || renderApi == GraphicsApi.SOFTWARE_COMPAT)
    }

    override val description: String =
        when (renderApi) {
            GraphicsApi.SOFTWARE_FAST -> "Skia Raster/Win32 GDI"
            else -> "Skia Raster/Win32 GDI (compatibility)"
        }

    override val deviceName: String = "Software"
    override val effectiveFrameBufferCount: Int = 1

    private var bitmap: Bitmap? = null
    private var pixmap: Pixmap? = null
    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var closed = false

    override fun render(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ) {
        check(!closed) { "Renderer is closed" }
        ensureSurface(width, height)
        val rasterSurface = checkNotNull(surface)
        rasterSurface.canvas.clear(Color.TRANSPARENT)
        block(rasterSurface.canvas)
        rasterSurface.flush()
        val pixels = checkNotNull(pixmap)
        window.presentSoftwareFrame(pixels.addr, width, height, pixels.rowBytes)
    }

    override fun snapshot(width: Int, height: Int): Bitmap {
        check(!closed) { "Renderer is closed" }
        ensureSurface(width, height)
        val result = Bitmap()
        check(result.allocPixels(imageInfo(width, height))) {
            "Could not allocate a Skia screenshot bitmap"
        }
        if (!checkNotNull(surface).readPixels(result, 0, 0)) {
            result.close()
            error("Could not read pixels from the Skia raster surface")
        }
        return result
    }

    private fun ensureSurface(newWidth: Int, newHeight: Int) {
        require(newWidth > 0 && newHeight > 0)
        if (surface != null && width == newWidth && height == newHeight) return

        closeSurface()
        width = newWidth
        height = newHeight
        bitmap =
            Bitmap().also {
                check(it.allocPixels(imageInfo(width, height))) {
                    "Could not allocate a Skia software framebuffer"
                }
            }
        pixmap = checkNotNull(bitmap?.peekPixels())
        surface =
            Surface.makeRasterDirect(
                pixmap = checkNotNull(pixmap),
                surfaceProps = SurfaceProps(pixelGeometry = pixelGeometry),
            )
    }

    override fun close(contextLost: Boolean) {
        if (closed) return
        closed = true
        closeSurface()
    }

    private fun closeSurface() {
        surface?.close()
        surface = null
        pixmap?.close()
        pixmap = null
        bitmap?.close()
        bitmap = null
    }

    private fun imageInfo(width: Int, height: Int): ImageInfo =
        ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL, ColorSpace.sRGB)
}
