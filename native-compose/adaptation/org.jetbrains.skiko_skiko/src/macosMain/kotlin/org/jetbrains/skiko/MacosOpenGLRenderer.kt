@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.BackendTexture
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skia.impl.Native.Companion.NullPointer

internal class MacosOpenGLRenderer(
    private val component: MacosSkiaLayerComponent,
) : MacosNativeRenderer {
    private val openGlContext =
        component.createOpenGlContext().also {
            if (it == NullPointer) throw RenderException("Could not create an OpenGL context")
        }

    private val directContext: DirectContext
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var closed = false

    val deviceName: String?
    override val description: String

    init {
        try {
            component.makeOpenGlContextCurrent(openGlContext)
            directContext = DirectContext.makeGL()
            deviceName = component.openGlRendererName()
            description =
                if (deviceName.isNullOrBlank()) "Skia Ganesh/OpenGL (macOS SDL)"
                else "Skia Ganesh/OpenGL (macOS SDL, $deviceName)"
        } catch (failure: Throwable) {
            component.deleteOpenGlContext(openGlContext)
            throw if (failure is RenderException) failure
            else RenderException("Could not initialize the macOS OpenGL renderer", failure)
        }
    }

    override fun render(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ): Boolean {
        makeCurrent()
        component.setOpenGlSwapInterval(if (waitForVsync) 1 else 0)
        ensureSurface(width, height)
        val skiaSurface = checkNotNull(surface)
        skiaSurface.canvas.clear(Color.TRANSPARENT)
        block(skiaSurface.canvas)
        skiaSurface.flushAndSubmit()
        component.swapOpenGlBuffers()
        return true
    }

    fun <T> withExternalOpenGl(block: () -> T): T {
        makeCurrent()
        surface?.flushAndSubmit()
        return try {
            block()
        } finally {
            directContext.resetGLAll()
        }
    }

    fun drawTexture(textureId: Int, width: Int, height: Int, canvas: Canvas) {
        require(textureId != 0 && width > 0 && height > 0)
        makeCurrent()
        val texture = BackendTexture.makeGL(
            width = width,
            height = height,
            isMipmapped = false,
            textureId = textureId,
            textureTarget = GL_TEXTURE_2D,
            textureFormat = GL_RGBA8,
        )
        val image = try {
            Image.borrowTextureFrom(
                context = directContext,
                backendTexture = texture,
                origin = SurfaceOrigin.BOTTOM_LEFT,
                colorType = ColorType.RGBA_8888,
                alphaType = ColorAlphaType.PREMUL,
            )
        } catch (failure: Throwable) {
            texture.close()
            throw failure
        }
        try {
            canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()), null)
        } finally {
            image.close()
            texture.close()
        }
    }

    fun snapshot(width: Int, height: Int): Bitmap {
        makeCurrent()
        ensureSurface(width, height)
        val bitmap = Bitmap()
        check(bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))) {
            "Could not allocate a Skia screenshot bitmap"
        }
        if (!checkNotNull(surface).readPixels(bitmap, 0, 0)) {
            bitmap.close()
            error("Could not read pixels from the Skia window surface")
        }
        return bitmap
    }

    override fun close() {
        if (closed) return
        makeCurrent()
        closed = true
        surface?.close()
        surface = null
        renderTarget?.close()
        renderTarget = null
        directContext.close()
        component.deleteOpenGlContext(openGlContext)
    }

    private fun makeCurrent() {
        check(!closed) { "Renderer is closed" }
        component.makeOpenGlContextCurrent(openGlContext)
    }

    private fun ensureSurface(newWidth: Int, newHeight: Int) {
        require(newWidth > 0 && newHeight > 0)
        if (surface != null && width == newWidth && height == newHeight) return

        surface?.close()
        surface = null
        renderTarget?.close()
        renderTarget = null

        width = newWidth
        height = newHeight
        renderTarget = BackendRenderTarget.makeGL(
            width = width,
            height = height,
            sampleCnt = 0,
            stencilBits = 8,
            fbId = 0,
            fbFormat = FramebufferFormat.GR_GL_RGBA8,
        )
        surface = Surface.makeFromBackendRenderTarget(
            context = directContext,
            rt = checkNotNull(renderTarget),
            origin = SurfaceOrigin.BOTTOM_LEFT,
            colorFormat = SurfaceColorFormat.RGBA_8888,
            colorSpace = ColorSpace.sRGB,
            surfaceProps = SurfaceProps(pixelGeometry = component.pixelGeometry),
        ) ?: throw RenderException("Skia could not wrap the macOS SDL OpenGL framebuffer")
    }

    private companion object {
        const val GL_TEXTURE_2D = 0x0DE1
        const val GL_RGBA8 = 0x8058
    }
}
