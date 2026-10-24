@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, org.jetbrains.skiko.InternalSkikoApi::class)

package love.yinlin.nativecompose.window

import org.jetbrains.skia.Canvas

import kotlinx.cinterop.*
import love.yinlin.nativecompose.sdl.*
import org.jetbrains.skia.impl.Native.Companion.NullPointer
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.*

internal actual fun gpuWindowFlags(): ULong = SDL_WINDOW_VULKAN
internal actual fun createGpuRenderer(window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit): WindowRenderer = LinuxGpuRenderer(window, draw)

internal class LinuxGpuRenderer(private val window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit) : WindowRenderer, LinuxSkiaLayerComponent {
    private val geometry = SdlWindowGeometry(window)
    private val layer = SkiaLayer(SkiaLayerProperties(renderApi = GraphicsApi.VULKAN))
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
            layer.renderDelegate = SkikoRenderDelegate { canvas, width, height, _ -> draw(canvas, width, height) }
            layer.attachTo(this)
            check(layer.renderApi == GraphicsApi.VULKAN) { "Vulkan initialization fell back to ${layer.renderApi}" }
        }
        catch (failure: Throwable) {
            layer.detach()
            throw failure
        }
    }

    override fun createVulkanWindow(properties: SkiaLayerProperties): LinuxVulkanWindow = memScoped {
        val count = alloc<UIntVar>()
        val names = checkNotNull(SDL_Vulkan_GetInstanceExtensions(count.ptr)) { SDL_GetError()?.toKString() ?: "No Vulkan extensions" }
        check(count.value in 1u .. 64u)
        val extensions = List(count.value.toInt()) { index -> checkNotNull(names[index]).toKString() }
        LinuxVulkanWindow(extensions, checkNotNull(RachelSDL_VulkanSurfaceFactory()).rawValue, window.rawValue, properties)
    }

    override fun render(width: Int, height: Int): Boolean {
        val rendered = layer.render(force = true)
        check(layer.renderApi == GraphicsApi.VULKAN) { "Vulkan rendering fell back to ${layer.renderApi}" }
        return rendered
    }

    override fun requestRender() = RachelSDL_RequestRender(window)
    override fun close() = layer.detach()

    // This SDL window was created for Vulkan; fallback is deliberately an error in this probe.
    override val openGlResolverContext: NativePointer get() = NullPointer
    override val openGlResolver: NativePointer get() = NullPointer
    override fun createOpenGlContext(): NativePointer = error("This probe requires Vulkan")
    override fun makeOpenGlContextCurrent(context: NativePointer): Unit = error("This probe requires Vulkan")
    override fun setOpenGlSwapInterval(interval: Int): Boolean = false
    override fun swapOpenGlBuffers(): Unit = error("This probe requires Vulkan")
    override fun deleteOpenGlContext(context: NativePointer) = Unit
}
