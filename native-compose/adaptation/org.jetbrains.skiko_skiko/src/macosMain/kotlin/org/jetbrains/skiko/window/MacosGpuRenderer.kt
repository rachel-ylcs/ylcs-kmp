package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.*
import platform.QuartzCore.CAMetalLayer

internal actual fun gpuWindowFlags(): ULong = SdlMetalWindow.flags
internal actual fun createGpuRenderer(window: COpaquePointer, transparent: Boolean, draw: (Canvas, Int, Int) -> Unit): WindowRenderer = MacosGpuRenderer(window, transparent, draw)

internal class MacosGpuRenderer(private val window: COpaquePointer, private val transparent: Boolean, draw: (Canvas, Int, Int) -> Unit) : WindowRenderer, MacosSkiaLayerComponent {
    private val geometry = SdlWindowGeometry(window)
    private var metalView: COpaquePointer? = null
    private val layer = SkiaLayer()
    override val windowHandle: Any get() = window
    override val drawableWidth: Int get() = geometry.width
    override val drawableHeight: Int get() = geometry.height
    override val contentScale: Float get() = geometry.scale
    override val isShowing: Boolean get() = geometry.isShowing
    override var fullscreen: Boolean
        get() = geometry.fullscreen
        set(value) { geometry.fullscreen = value }
    override val description: String get() = layer.rendererDescription

    init {
        try {
            layer.renderApi = GraphicsApi.METAL
            layer.renderDelegate = SkikoRenderDelegate { canvas, width, height, _ -> draw(canvas, width, height) }
            layer.attachTo(this)
        }
        catch (failure: Throwable) {
            layer.detach()
            deleteMetalLayer()
            throw failure
        }
    }

    override fun createMetalLayer(): CAMetalLayer {
        check(metalView == null)
        val view = SdlMetalWindow.createView(window)
        metalView = view
        val pointer = SdlMetalWindow.layer(view)
        return interpretObjCPointer<CAMetalLayer>(pointer.rawValue).apply { opaque = !transparent }
    }

    override fun deleteMetalLayer() {
        metalView?.let { SdlMetalWindow.destroyView(it) }
        metalView = null
    }

    override fun render(width: Int, height: Int): Boolean {
        check(layer.renderApi == GraphicsApi.METAL)
        return layer.render(force = true)
    }

    override fun requestRender() = SdlMetalWindow.requestRender(window)
    override fun close() = layer.detach()
    override fun createOpenGlContext(): NativePointer = error("This probe requires Metal")
    override fun makeOpenGlContextCurrent(context: NativePointer): Unit = error("This probe requires Metal")
    override fun setOpenGlSwapInterval(interval: Int): Boolean = false
    override fun swapOpenGlBuffers(): Unit = error("This probe requires Metal")
    override fun deleteOpenGlContext(context: NativePointer) = Unit
}
