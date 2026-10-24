package org.jetbrains.skiko

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface

internal class LinuxVulkanRenderer(
    private val component: LinuxSkiaLayerComponent,
    private val properties: SkiaLayerProperties,
) : LinuxLayerRenderer {
    private val window = component.createVulkanWindow(properties)
    private var surface: Surface? = null
    private var closed = false

    override val renderApi = GraphicsApi.VULKAN
    override val deviceName = window.deviceName
    override val description = "Skia Ganesh/Vulkan ($deviceName)"

    override fun render(width: Int, height: Int, waitForVsync: Boolean, block: (Canvas) -> Unit) {
        check(!closed) { "Renderer is closed" }
        surface?.close()
        surface = null
        val frame = window.acquire(width, height, properties.isVsyncEnabled && waitForVsync)
        surface = frame
        frame.canvas.clear(if (component.transparency) Color.TRANSPARENT else Color.BLACK)
        block(frame.canvas)
        window.present()
    }

    override fun snapshot(width: Int, height: Int): Bitmap {
        val frame = checkNotNull(surface) { "Render a Vulkan frame before taking a snapshot" }
        require(width == frame.width && height == frame.height) { "Snapshot size must match the last rendered frame" }
        val bitmap = Bitmap()
        try {
            check(bitmap.allocPixels(ImageInfo.makeN32Premul(width, height)))
            check(frame.readPixels(bitmap, 0, 0)) { "Could not read pixels from the Vulkan scene surface" }
            return bitmap
        }
        catch (failure: Throwable) {
            bitmap.close()
            throw failure
        }
    }

    override fun isContextLost(): Boolean = window.isContextLost()

    override fun close(contextLost: Boolean) {
        if (closed) return
        closed = true
        try {
            surface?.close()
            surface = null
        }
        finally { window.close() }
    }
}