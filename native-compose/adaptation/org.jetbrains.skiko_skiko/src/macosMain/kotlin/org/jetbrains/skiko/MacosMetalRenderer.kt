@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.objcPtr
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import platform.CoreGraphics.CGSizeMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalDrawableProtocol
import platform.QuartzCore.CAMetalLayer

internal class MacosMetalRenderer(
    private val component: MacosSkiaLayerComponent,
) : MacosNativeRenderer {
    private val metalLayer: CAMetalLayer = component.createMetalLayer()
    private val device =
        MTLCreateSystemDefaultDevice()
            ?: run {
                component.deleteMetalLayer()
                throw RenderException("Metal is not supported on this system")
            }
    private val queue =
        device.newCommandQueue()
            ?: run {
                component.deleteMetalLayer()
                throw RenderException("Could not create the Metal command queue")
            }
    private val directContext: DirectContext
    private var closed = false

    override val description: String
        get() = "Skia Ganesh/Metal (macOS native host, ${device.name})"

    init {
        try {
            metalLayer.device = device as objcnames.protocols.MTLDeviceProtocol?
            metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm
            metalLayer.framebufferOnly = false
            metalLayer.contentsScale = component.contentScale.toDouble()
            metalLayer.drawableSize =
                CGSizeMake(
                    component.drawableWidth.coerceAtLeast(1).toDouble(),
                    component.drawableHeight.coerceAtLeast(1).toDouble(),
                )
            directContext = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
        } catch (failure: Throwable) {
            component.deleteMetalLayer()
            throw if (failure is RenderException) failure
            else RenderException("Could not initialize the macOS Metal renderer", failure)
        }
    }

    override fun render(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ): Boolean {
        check(!closed) { "Renderer is closed" }
        require(width > 0 && height > 0)

        return autoreleasepool {
            metalLayer.contentsScale = component.contentScale.toDouble()
            metalLayer.drawableSize = CGSizeMake(width.toDouble(), height.toDouble())
            metalLayer.displaySyncEnabled = waitForVsync

            val drawable = metalLayer.nextDrawable() ?: return@autoreleasepool false
            renderDrawable(drawable, width, height, block)
            true
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            metalLayer.displaySyncEnabled = false
            directContext.close()
        } finally {
            component.deleteMetalLayer()
        }
    }

    private fun renderDrawable(
        drawable: CAMetalDrawableProtocol,
        width: Int,
        height: Int,
        block: (Canvas) -> Unit,
    ) {
        val renderTarget = BackendRenderTarget.makeMetal(width, height, drawable.texture.objcPtr())
        try {
            val surface =
                Surface.makeFromBackendRenderTarget(
                    context = directContext,
                    rt = renderTarget,
                    origin = SurfaceOrigin.TOP_LEFT,
                    colorFormat = SurfaceColorFormat.BGRA_8888,
                    colorSpace = ColorSpace.sRGB,
                    surfaceProps = SurfaceProps(pixelGeometry = component.pixelGeometry),
                ) ?: throw RenderException("Skia could not wrap the macOS Metal drawable")
            try {
                surface.canvas.clear(Color.TRANSPARENT)
                block(surface.canvas)
                surface.flushAndSubmit()
            } finally {
                surface.close()
            }

            val commandBuffer =
                queue.commandBuffer()
                    ?: throw RenderException("Could not create a Metal presentation command buffer")
            commandBuffer.presentDrawable(drawable)
            commandBuffer.commit()
        } finally {
            renderTarget.close()
        }
    }
}
