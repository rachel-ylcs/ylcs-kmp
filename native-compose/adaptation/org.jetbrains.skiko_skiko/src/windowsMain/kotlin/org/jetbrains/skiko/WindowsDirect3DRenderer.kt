@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.toKString
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps
import org.jetbrains.skia.impl.NativePointer
import kotlin.native.internal.NativePtr

private const val Direct3DRenderTargetGrowthAlignment = 64L

internal data class Direct3DRenderTargetSize(val width: Int, val height: Int)

internal data class Direct3DCompositionScale(val x: Float, val y: Float)

internal fun direct3DCompositionScale(
    sourceWidth: Int,
    sourceHeight: Int,
    renderTargetWidth: Int,
    renderTargetHeight: Int,
): Direct3DCompositionScale {
    require(
        sourceWidth > 0 &&
            sourceHeight > 0 &&
            renderTargetWidth >= sourceWidth &&
            renderTargetHeight >= sourceHeight,
    )
    return Direct3DCompositionScale(
        x = sourceWidth.toFloat() / renderTargetWidth.toFloat(),
        y = sourceHeight.toFloat() / renderTargetHeight.toFloat(),
    )
}

internal fun direct3DRenderTargetSize(
    currentWidth: Int,
    currentHeight: Int,
    requestedWidth: Int,
    requestedHeight: Int,
): Direct3DRenderTargetSize {
    require(requestedWidth > 0 && requestedHeight > 0)
    return Direct3DRenderTargetSize(
        width = expandedRenderTargetDimension(currentWidth, requestedWidth),
        height = expandedRenderTargetDimension(currentHeight, requestedHeight),
    )
}

private fun expandedRenderTargetDimension(current: Int, requested: Int): Int {
    if (current >= requested) return current
    val currentLong = current.toLong()
    val requestedLong = requested.toLong()
    val expanded =
        if (current <= 0) {
            requestedLong + maxOf(requestedLong / 2L, Direct3DRenderTargetGrowthAlignment)
        } else {
            maxOf(
                requestedLong,
                currentLong + maxOf(currentLong / 2L, Direct3DRenderTargetGrowthAlignment),
            )
        }
    return (
            (expanded + Direct3DRenderTargetGrowthAlignment - 1L) /
                Direct3DRenderTargetGrowthAlignment * Direct3DRenderTargetGrowthAlignment
        )
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}

/** D3D12/Ganesh renderer for a native Win32-backed [SkiaLayer]. */
internal class WindowsDirect3DRenderer(
    private val window: WindowsNativeWindow,
    private val properties: SkiaLayerProperties,
    private val pixelGeometry: PixelGeometry,
    private val transparencyRequested: Boolean,
) : WindowsLayerRenderer {
    override val renderApi: GraphicsApi = GraphicsApi.DIRECT3D
    override val effectiveFrameBufferCount: Int =
        properties.frameBuffering.numberOfBuffers() ?: DefaultBufferCount

    private var nativeDevice: NativePointer = NativePtr.NULL
    private var directContext: DirectContext? = null
    private val renderTargets = arrayOfNulls<BackendRenderTarget>(effectiveFrameBufferCount)
    private val surfaces = arrayOfNulls<Surface>(effectiveFrameBufferCount)
    private var width = 0
    private var height = 0
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var lastRenderedBufferIndex = -1
    private var closed = false
    private var contextFailed = false

    override var deviceName: String? = null
        private set

    private var adapterMemory: Long = 0L

    override val description: String
        get() = buildString {
            append("Skia Ganesh/Direct3D 12")
            deviceName?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
            if (adapterMemory > 0) append(" [${adapterMemory / BytesPerMiB} MiB]")
        }

    override val transparencySupported: Boolean
        get() =
            nativeDevice != NativePtr.NULL &&
                windowsDirect3DTransparencySupported(nativeDevice)

    init {
        initialize()
    }

    private fun initialize() {
        nativeDevice =
            windowsDirect3DCreate(
                window = window.hwnd().rawValue,
                adapterPriority = properties.adapterPriority.ordinal,
                bufferCount = effectiveFrameBufferCount,
                transparency = transparencyRequested,
            )
        if (nativeDevice == NativePtr.NULL) {
            throw RenderException(
                "Could not initialize Direct3D 12 (${formatHResult(windowsDirect3DLastCreateError())})",
            )
        }
        try {
            deviceName =
                windowsDirect3DAdapterName(nativeDevice)
                    .takeUnless { it == NativePtr.NULL }
                    ?.let { interpretCPointer<ByteVar>(it)?.toKString() }
            if (deviceName?.let { isVideoCardSupported(GraphicsApi.DIRECT3D, hostOs, it) } == false) {
                throw RenderException("Direct3D adapter is not supported: $deviceName")
            }
            adapterMemory = windowsDirect3DAdapterMemory(nativeDevice)
            directContext =
                DirectContext.makeDirect3D(
                    adapterPtr = requiredNativePointer(windowsDirect3DAdapter(nativeDevice), "adapter"),
                    devicePtr = requiredNativePointer(windowsDirect3DDevice(nativeDevice), "device"),
                    queuePtr = requiredNativePointer(windowsDirect3DQueue(nativeDevice), "command queue"),
                ).also { context ->
                    if (properties.gpuResourceCacheLimit >= 0L) {
                        context.resourceCacheLimit = properties.gpuResourceCacheLimit
                    }
                }
        } catch (failure: Throwable) {
            contextFailed = true
            releaseResources(contextLost = true, propagateFailure = false)
            throw if (failure is RenderException) failure
            else RenderException("Could not initialize the Direct3D renderer", failure)
        }
    }

    override fun render(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ) {
        checkRendererOpen()
        val targetSize =
            direct3DRenderTargetSize(
                currentWidth = this.width,
                currentHeight = this.height,
                requestedWidth = width,
                requestedHeight = height,
            )
        val bufferIndex = acquireSurface(targetSize.width, targetSize.height)
        val surface = checkNotNull(surfaces[bufferIndex])
        val canvas = surface.canvas
        // Clear the full capacity so newly exposed spare pixels never contain an older layout.
        canvas.clear(Color.TRANSPARENT)
        val saveCount = canvas.save()
        try {
            // The swap-chain buffers retain spare capacity so ordinary window resizes only change
            // the visible source rectangle. Keep Compose and Skia in the same pixel coordinate
            // space and clip work outside the current client area.
            canvas.clipRect(Rect.makeWH(width.toFloat(), height.toFloat()))
            block(canvas)
        } finally {
            canvas.restoreToCount(saveCount)
        }
        surface.flushAndSubmit()
        // SetSourceSize takes effect immediately, independently of Present. Update it only after
        // the replacement frame is ready so DWM never stretches the previously presented layout
        // while Compose is still drawing the new size.
        setSourceSize(width, height)

        val result =
            windowsDirect3DPresent(
                nativeDevice,
                waitForVsync && properties.isVsyncEnabled,
            )
        if (result < 0) {
            contextFailed = windowsDirect3DIsDeviceLost(nativeDevice)
            throw RenderException("Direct3D Present failed (${formatHResult(result)})")
        }
        lastRenderedBufferIndex = bufferIndex
    }

    override fun snapshot(width: Int, height: Int): Bitmap {
        checkRendererOpen()
        val bufferIndex =
            if (
                width == sourceWidth &&
                height == sourceHeight &&
                lastRenderedBufferIndex in surfaces.indices &&
                surfaces[lastRenderedBufferIndex] != null
            ) {
                lastRenderedBufferIndex
            } else {
                val targetSize =
                    direct3DRenderTargetSize(
                        currentWidth = this.width,
                        currentHeight = this.height,
                        requestedWidth = width,
                        requestedHeight = height,
                    )
                acquireSurface(targetSize.width, targetSize.height).also {
                    setSourceSize(width, height)
                }
            }
        val bitmap = Bitmap()
        check(bitmap.allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL))) {
            "Could not allocate a Skia screenshot bitmap"
        }
        if (!checkNotNull(surfaces[bufferIndex]).readPixels(bitmap, 0, 0)) {
            bitmap.close()
            error("Could not read pixels from the Direct3D window surface")
        }
        return bitmap
    }

    private fun acquireSurface(newWidth: Int, newHeight: Int): Int {
        require(newWidth > 0 && newHeight > 0)
        ensureSwapChain(newWidth, newHeight)
        val resource = windowsDirect3DAcquireBuffer(nativeDevice)
        if (resource == NativePtr.NULL) {
            contextFailed = windowsDirect3DIsDeviceLost(nativeDevice)
            throw RenderException(
                "Could not acquire a Direct3D swap-chain buffer " +
                    "(${formatHResult(windowsDirect3DLastError(nativeDevice))})",
            )
        }
        val bufferIndex = windowsDirect3DCurrentBufferIndex(nativeDevice)
        check(bufferIndex in surfaces.indices) {
            "Direct3D returned invalid swap-chain buffer index $bufferIndex"
        }
        if (surfaces[bufferIndex] == null) {
            val renderTarget =
                BackendRenderTarget.makeDirect3D(
                    width = width,
                    height = height,
                    texturePtr = resource,
                    format = DxgiFormatR8G8B8A8Unorm,
                    sampleCnt = 1,
                    levelCnt = 1,
                )
            val surface =
                Surface.makeFromBackendRenderTarget(
                    context = checkNotNull(directContext),
                    rt = renderTarget,
                    origin = SurfaceOrigin.TOP_LEFT,
                    colorFormat = SurfaceColorFormat.RGBA_8888,
                    colorSpace = ColorSpace.sRGB,
                    surfaceProps = SurfaceProps(pixelGeometry = pixelGeometry),
                )
            if (surface == null) {
                renderTarget.close()
                throw RenderException("Skia could not wrap a Direct3D swap-chain buffer")
            }
            renderTargets[bufferIndex] = renderTarget
            surfaces[bufferIndex] = surface
        }
        return bufferIndex
    }

    private fun setSourceSize(newWidth: Int, newHeight: Int) {
        if (sourceWidth == newWidth && sourceHeight == newHeight) {
            return
        }
        val scale = direct3DCompositionScale(newWidth, newHeight, width, height)
        val result =
            windowsDirect3DSetSourceSize(
                handle = nativeDevice,
                width = newWidth,
                height = newHeight,
                scaleX = scale.x,
                scaleY = scale.y,
            )
        if (result < 0) {
            contextFailed = windowsDirect3DIsDeviceLost(nativeDevice)
            throw RenderException(
                "Could not update the Direct3D swap-chain source size (${formatHResult(result)})",
            )
        }
        sourceWidth = newWidth
        sourceHeight = newHeight
    }

    private fun ensureSwapChain(newWidth: Int, newHeight: Int) {
        val isResize = width != 0 && (width != newWidth || height != newHeight)
        if (!isResize && width == newWidth && height == newHeight) return
        if (isResize) {
            closeSurfaces()
            directContext?.flush()
        }
        val result =
            windowsDirect3DEnsureSwapChain(
                handle = nativeDevice,
                width = newWidth,
                height = newHeight,
                resize = isResize,
            )
        if (result < 0) {
            contextFailed = windowsDirect3DIsDeviceLost(nativeDevice)
            throw RenderException(
                "Could not ${if (isResize) "resize" else "create"} the Direct3D swap chain " +
                    "(${formatHResult(result)})",
            )
        }
        width = newWidth
        height = newHeight
        sourceWidth = 0
        sourceHeight = 0
        lastRenderedBufferIndex = -1
    }

    override fun isContextLost(): Boolean {
        if (closed || contextFailed || !window.isValid || nativeDevice == NativePtr.NULL) return true
        contextFailed = windowsDirect3DIsDeviceLost(nativeDevice)
        return contextFailed
    }

    override fun close(contextLost: Boolean) {
        releaseResources(contextLost = contextLost, propagateFailure = true)
    }

    private fun releaseResources(contextLost: Boolean, propagateFailure: Boolean) {
        if (closed) return
        closed = true
        val lost = contextLost || contextFailed || !window.isValid
        var cleanupFailure: Throwable? = null

        fun cleanup(block: () -> Unit) {
            try {
                block()
            } catch (failure: Throwable) {
                if (cleanupFailure == null) cleanupFailure = failure
            }
        }

        cleanup(::closeSurfaces)
        directContext?.let { context ->
            if (lost) cleanup(context::abandon)
            cleanup(context::close)
        }
        directContext = null
        if (nativeDevice != NativePtr.NULL) {
            windowsDirect3DClose(nativeDevice, lost)
            nativeDevice = NativePtr.NULL
        }
        if (propagateFailure) {
            cleanupFailure?.let { throw RenderException("Could not fully release Direct3D", it) }
        }
    }

    private fun closeSurfaces() {
        surfaces.indices.forEach { index ->
            surfaces[index]?.close()
            surfaces[index] = null
            renderTargets[index]?.close()
            renderTargets[index] = null
        }
        lastRenderedBufferIndex = -1
    }

    private fun checkRendererOpen() {
        check(!closed && nativeDevice != NativePtr.NULL) { "Renderer is closed" }
        if (windowsDirect3DIsDeviceLost(nativeDevice)) {
            contextFailed = true
            throw RenderException(
                "The Direct3D device was lost " +
                    "(${formatHResult(windowsDirect3DLastError(nativeDevice))})",
            )
        }
    }

    private fun requiredNativePointer(pointer: NativePointer, name: String): NativePointer {
        if (pointer == NativePtr.NULL) {
            throw RenderException("Direct3D returned a null $name")
        }
        return pointer
    }

    private companion object {
        const val DefaultBufferCount = 2
        const val DxgiFormatR8G8B8A8Unorm = 28
        const val BytesPerMiB = 1024L * 1024L
    }
}

private fun formatHResult(value: Int): String = "HRESULT 0x${value.toUInt().toString(16).padStart(8, '0')}"

@ExternalSymbolName("skiko_windows_d3d_create")
private external fun windowsDirect3DCreate(
    window: NativePointer,
    adapterPriority: Int,
    bufferCount: Int,
    transparency: Boolean,
): NativePointer

@ExternalSymbolName("skiko_windows_d3d_last_create_error")
private external fun windowsDirect3DLastCreateError(): Int

@ExternalSymbolName("skiko_windows_d3d_adapter")
private external fun windowsDirect3DAdapter(handle: NativePointer): NativePointer

@ExternalSymbolName("skiko_windows_d3d_device")
private external fun windowsDirect3DDevice(handle: NativePointer): NativePointer

@ExternalSymbolName("skiko_windows_d3d_queue")
private external fun windowsDirect3DQueue(handle: NativePointer): NativePointer

@ExternalSymbolName("skiko_windows_d3d_adapter_name")
private external fun windowsDirect3DAdapterName(handle: NativePointer): NativePointer

@ExternalSymbolName("skiko_windows_d3d_adapter_memory")
private external fun windowsDirect3DAdapterMemory(handle: NativePointer): Long

@ExternalSymbolName("skiko_windows_d3d_ensure_swap_chain")
private external fun windowsDirect3DEnsureSwapChain(
    handle: NativePointer,
    width: Int,
    height: Int,
    resize: Boolean,
): Int

@ExternalSymbolName("skiko_windows_d3d_set_source_size")
private external fun windowsDirect3DSetSourceSize(
    handle: NativePointer,
    width: Int,
    height: Int,
    scaleX: Float,
    scaleY: Float,
): Int

@ExternalSymbolName("skiko_windows_d3d_acquire_buffer")
private external fun windowsDirect3DAcquireBuffer(handle: NativePointer): NativePointer

@ExternalSymbolName("skiko_windows_d3d_current_buffer_index")
private external fun windowsDirect3DCurrentBufferIndex(handle: NativePointer): Int

@ExternalSymbolName("skiko_windows_d3d_present")
private external fun windowsDirect3DPresent(handle: NativePointer, waitForVsync: Boolean): Int

@ExternalSymbolName("skiko_windows_d3d_transparency_supported")
private external fun windowsDirect3DTransparencySupported(handle: NativePointer): Boolean

@ExternalSymbolName("skiko_windows_d3d_is_device_lost")
private external fun windowsDirect3DIsDeviceLost(handle: NativePointer): Boolean

@ExternalSymbolName("skiko_windows_d3d_last_error")
private external fun windowsDirect3DLastError(handle: NativePointer): Int

@ExternalSymbolName("skiko_windows_d3d_close")
private external fun windowsDirect3DClose(handle: NativePointer, contextLost: Boolean)
