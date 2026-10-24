@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package org.jetbrains.skiko.redrawer

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import org.jetbrains.skiko.*
import platform.CoreFoundation.CFTimeInterval
import platform.CoreGraphics.CGRectMake
import platform.CoreVideo.CVTimeStamp
import platform.OpenGL.GL_DRAW_FRAMEBUFFER_BINDING
import platform.OpenGL.glGetIntegerv
import platform.OpenGLCommon.CGLContextObj
import platform.OpenGLCommon.CGLPixelFormatObj
import platform.OpenGLCommon.CGLSetCurrentContext
import platform.OpenGLCommon.GLenum
import platform.QuartzCore.CAOpenGLLayer
import platform.QuartzCore.*

/**
 * OpenGL [Redrawer] implementation for the native (Kotlin/Native) macOS target.
 */
internal class MacOsOpenGLRedrawer(
    private val skiaLayer: SkiaLayer
) : Redrawer {
    private var context: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var canvas: Canvas? = null

    private var currentWidth = 0
    private var currentHeight = 0

    private val glLayer = MacosGLLayer()

    init {
        glLayer.init(skiaLayer, this)
    }

    override val renderInfo: String get() = "GraphicsApi: ${skiaLayer.renderApi}\nOS: macOS (native OpenGL)\n"

    private val frameDispatcher = FrameDispatcher(SkikoDispatchers.Main) {
        renderImmediately()
    }

    override fun dispose() {
        disposeSurface()
        context?.close()
        context = null
        glLayer.dispose()
    }

    override fun syncBoundsFromPlatformComponent() {
        syncContentScale()
        skiaLayer.nsView.frame.useContents {
            glLayer.setFrame(
                origin.x.toInt(),
                origin.y.toInt(),
                size.width.toInt().coerceAtLeast(0),
                size.height.toInt().coerceAtLeast(0)
            )
        }
    }

    private fun syncContentScale() {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        glLayer.contentsScale = skiaLayer.nsView.window?.backingScaleFactor ?: 1.0
        CATransaction.commit()
        CATransaction.flush()
    }

    override fun update(nanoTime: Long) {
        skiaLayer.update(nanoTime)
    }

    override fun needRender(throttledToVsync: Boolean) {
        frameDispatcher.scheduleFrame()
    }

    override fun renderImmediately() {
        glLayer.setNeedsDisplay()
        skiaLayer.nsView.setNeedsDisplay(true)
    }

    internal fun drawInLayerContext() {
        skiaLayer.update(currentNanoTime())
        skiaLayer.inDrawScope {
            performDraw()
        }
    }

    private fun LayerDrawScope.performDraw() {
        if (!initContext()) {
            throw RenderException("Cannot init graphic OpenGL context")
        }
        initSurface()
        canvas?.runRestoringState {
            clear(Color.TRANSPARENT)
            skiaLayer.draw(this)
        }
        context?.flush()
    }

    private fun initContext(): Boolean {
        try {
            if (context == null) {
                context = DirectContext.makeGL()
            }
        }
        catch (_: Exception) {
            println("Failed to create Skia OpenGL context!")
            return false
        }
        return true
    }

    @ExperimentalUnsignedTypes
    private fun openglGetIntegerv(pname: GLenum): UInt {
        var result = 0U
        memScoped {
            val data = alloc<IntVar>()
            glGetIntegerv(pname, data.ptr)
            result = data.value.toUInt()
        }
        return result
    }

    private fun isSizeChanged(width: Int, height: Int): Boolean {
        if (width != currentWidth || height != currentHeight) {
            currentWidth = width
            currentHeight = height
            return true
        }
        return false
    }

    private fun LayerDrawScope.initSurface() {
        val w = scaledLayerWidth
        val h = scaledLayerHeight
        if (isSizeChanged(w, h) || surface == null) {
            disposeSurface()
            if (w <= 0 || h <= 0) return
            val fbId = openglGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING.toUInt())
            renderTarget = BackendRenderTarget.makeGL(
                w,
                h,
                0,
                8,
                fbId.toInt(),
                FramebufferFormat.GR_GL_RGBA8
            )
            surface = Surface.makeFromBackendRenderTarget(
                context!!,
                renderTarget!!,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = skiaLayer.pixelGeometry)
            ) ?: throw RenderException("Cannot create surface")

            canvas = surface?.canvas ?: error("Could not obtain Canvas from Surface")
        }
    }

    private fun disposeSurface() {
        surface?.close()
        surface = null
        renderTarget?.close()
        renderTarget = null
        canvas = null
    }
}