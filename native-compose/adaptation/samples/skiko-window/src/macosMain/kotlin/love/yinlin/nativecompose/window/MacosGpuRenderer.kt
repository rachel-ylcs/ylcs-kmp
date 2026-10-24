@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class, org.jetbrains.skiko.InternalSkikoApi::class)

package love.yinlin.nativecompose.window

import org.jetbrains.skia.Canvas

import kotlinx.cinterop.*
import love.yinlin.nativecompose.sdl.*
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.*
import platform.QuartzCore.CAMetalLayer

internal actual fun gpuWindowFlags(): ULong = SDL_WINDOW_METAL
internal actual fun createGpuRenderer(window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit): WindowRenderer = MacosGpuRenderer(window, draw)

internal class MacosGpuRenderer(private val window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit) : WindowRenderer, MacosSkiaLayerComponent {
    private val geometry = SdlWindowGeometry(window)
    private var metalView: COpaquePointer? = null
    private val layer = SkiaLayer()
    override val windowHandle: Any get() = window
    override val drawableWidth: Int get() = geometry.width
    override val drawableHeight: Int get() = geometry.height
    override val contentScale: Float get() = geometry.scale
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
        val view = checkNotNull(RachelSDL_CreateMetalView(window)) { SDL_GetError()?.toKString() ?: "Cannot create SDL Metal view" }
        metalView = view
        val pointer = checkNotNull(RachelSDL_GetMetalLayer(view)) { "SDL did not expose a CAMetalLayer" }
        return interpretObjCPointer(pointer.rawValue)
    }

    override fun deleteMetalLayer() {
        metalView?.let { RachelSDL_DestroyMetalView(it) }
        metalView = null
    }

    override fun render(width: Int, height: Int): Boolean {
        check(layer.renderApi == GraphicsApi.METAL)
        return layer.render(force = true)
    }

    override fun requestRender() = RachelSDL_RequestRender(window)
    override fun close() = layer.detach()
    override fun createOpenGlContext(): NativePointer = error("This probe requires Metal")
    override fun makeOpenGlContextCurrent(context: NativePointer): Unit = error("This probe requires Metal")
    override fun setOpenGlSwapInterval(interval: Int): Boolean = false
    override fun swapOpenGlBuffers(): Unit = error("This probe requires Metal")
    override fun deleteOpenGlContext(context: NativePointer) = Unit
}
