@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, org.jetbrains.skiko.InternalSkikoApi::class)

package love.yinlin.nativecompose.window

import org.jetbrains.skia.Canvas

import kotlinx.cinterop.*
import love.yinlin.nativecompose.sdl.RachelSDL_GetWin32Window
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerProperties
import org.jetbrains.skiko.SkikoRenderDelegate

internal actual fun gpuWindowFlags(): ULong = 0uL
internal actual fun createGpuRenderer(window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit): WindowRenderer = WindowsGpuRenderer(window, draw)

internal class WindowsGpuRenderer(window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit) : WindowRenderer {
    private val layer = SkiaLayer(SkiaLayerProperties(renderApi = GraphicsApi.DIRECT3D))
    override val description: String
        get() = layer.rendererDescription

    init {
        try {
            val hwnd = checkNotNull(RachelSDL_GetWin32Window(window)) { "SDL did not expose a Win32 HWND" }
            layer.renderDelegate = SkikoRenderDelegate { canvas, width, height, _ -> draw(canvas, width, height) }
            layer.attachTo(hwnd.rawValue.toLong())
            check(layer.renderApi == GraphicsApi.DIRECT3D) { "D3D12 initialization fell back to ${layer.renderApi}" }
        }
        catch (failure: Throwable) {
            layer.detach()
            throw failure
        }
    }

    override fun render(width: Int, height: Int): Boolean {
        val rendered = layer.render(force = true)
        check(layer.renderApi == GraphicsApi.DIRECT3D) { "D3D12 rendering fell back to ${layer.renderApi}" }
        return rendered
    }

    override fun close() = layer.detach()
}
