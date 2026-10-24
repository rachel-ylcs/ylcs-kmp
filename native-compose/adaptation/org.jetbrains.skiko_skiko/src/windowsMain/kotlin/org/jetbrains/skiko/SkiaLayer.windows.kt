@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
    org.jetbrains.skiko.ExperimentalSkikoApi::class,
)

package org.jetbrains.skiko

import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PixelGeometry
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import platform.windows.GetMonitorInfoW
import platform.windows.GetWindowLongPtrW
import platform.windows.GetWindowRect
import platform.windows.GWL_STYLE
import platform.windows.HKEY_CURRENT_USER
import platform.windows.HWND
import platform.windows.MONITORINFO
import platform.windows.MONITOR_DEFAULTTONEAREST
import platform.windows.MonitorFromWindow
import platform.windows.RECT
import platform.windows.RegGetValueW
import platform.windows.RRF_RT_REG_DWORD
import platform.windows.SetWindowLongPtrW
import platform.windows.SetWindowPos
import platform.windows.ShowWindow
import platform.windows.SWP_FRAMECHANGED
import platform.windows.SWP_NOACTIVATE
import platform.windows.SWP_NOOWNERZORDER
import platform.windows.SWP_SHOWWINDOW
import platform.windows.SW_RESTORE
import platform.windows.WS_OVERLAPPEDWINDOW

private data class WindowRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Kotlin/Native Windows layer backed by a Win32 HWND. */
actual open class SkiaLayer(
    val properties: SkiaLayerProperties = SkiaLayerProperties(),
    private val analytics: SkiaLayerAnalytics = SkiaLayerAnalytics.Empty,
    actual val pixelGeometry: PixelGeometry = SkikoProperties.pixelGeometry,
) {
    private var nativeWindow: WindowsNativeWindow? = null
    private var renderer: WindowsLayerRenderer? = null
    private var rendererAnalytics: SkiaLayerAnalytics.RendererAnalytics? = null
    private var deviceAnalytics: SkiaLayerAnalytics.DeviceAnalytics? = null
    private var savedWindowStyle: Long? = null
    private var savedWindowRect: WindowRect? = null
    private var firstFrameRendered = false
    private val renderRequestState = AtomicInt(0)
    private val rendering = AtomicBoolean(false)
    private val renderWindow = AtomicReference<WindowsNativeWindow?>(null)
    private val renderDelegateReference = AtomicReference<SkikoRenderDelegate?>(null)
    private var detachAfterRender = false
    private var deferredRenderApi: GraphicsApi? = null
    private var fallbackCount = 0
    private var contextRecoveryCount = 0
    private var renderedFrameCount = 0L
    private var lastRenderFailure: String? = null

    /**
     * Whether the attached Win32 window should expose a composited buffer with per-pixel alpha.
     *
     * This must be configured before [attachTo], because a Win32 HWND's WGL pixel format can only
     * be assigned once. The host window must also have been created for transparency (for example,
     * with `SDL_WINDOW_TRANSPARENT`).
     */
    var transparency: Boolean = false
        set(value) {
            check(nativeWindow == null || field == value) {
                "SkiaLayer transparency must be configured before attaching to a Win32 HWND"
            }
            field = value
        }

    private val softwareFrameLimiter = WindowsFrameLimiter()
    private val fpsCounter =
        if (SkikoProperties.fpsEnabled) {
            FPSCounter(
                periodSeconds = SkikoProperties.fpsPeriodSeconds,
                showLongFrames = SkikoProperties.fpsLongFramesShow,
                getLongFrameMillis = {
                    SkikoProperties.fpsLongFramesMillis
                        ?: (1.5 * 1_000.0 / (nativeWindow?.displayRefreshRate ?: 60f))
                },
                logOnTick = true,
            )
        } else {
            null
        }

    private var activeRenderApi: GraphicsApi = properties.renderApi

    actual var renderApi: GraphicsApi
        get() = activeRenderApi
        set(value) {
            require(value in SupportedWindowsRenderApis) {
                "$value is not implemented by Kotlin/Native Windows"
            }
            if (activeRenderApi == value) return
            activeRenderApi = value
            if (nativeWindow != null) {
                if (rendering.load()) {
                    deferredRenderApi = value
                } else {
                    replaceRenderer(value, countAsFallback = false)
                    needRender(throttledToVsync = false)
                }
            }
        }

    actual val contentScale: Float
        get() = nativeWindow?.contentScale ?: 1f

    actual var fullscreen: Boolean
        get() = savedWindowStyle != null
        set(value) {
            val hwnd = nativeWindow?.hwnd()
                ?: throw IllegalStateException("SkiaLayer must be attached before changing fullscreen")
            if (value == fullscreen) return
            if (value) enterFullscreen(hwnd) else leaveFullscreen(hwnd)
        }

    actual val component: Any?
        get() = nativeWindow

    actual var renderDelegate: SkikoRenderDelegate?
        get() = renderDelegateReference.load()
        set(value) {
            renderDelegateReference.store(value)
            if (value != null && nativeWindow != null) needRender(throttledToVsync = false)
        }

    actual fun attachTo(container: Any) {
        check(nativeWindow == null) { "SkiaLayer is already attached" }
        val window =
            when (container) {
                is WindowsNativeWindow -> container
                is Long -> WindowsNativeWindow(container)
                else -> error("container must be WindowsNativeWindow or a non-zero HWND Long")
            }
        nativeWindow = window
        renderWindow.store(window)
        try {
            window.attachCallbacks(
                paint = { needRender(throttledToVsync = false) },
                render = { render() },
                onDestroyed = ::nativeWindowDestroyed,
                onFailure = ::recordWindowCallbackFailure,
            )
            replaceRenderer(renderApi, countAsFallback = false)
        } catch (failure: Throwable) {
            window.detachCallbacks()
            renderWindow.store(null)
            nativeWindow = null
            throw failure
        }
        if (renderDelegate != null) needRender(throttledToVsync = false)
    }

    actual fun detach() {
        if (rendering.load()) {
            detachAfterRender = true
            renderRequestState.store(0)
            return
        }
        detachNow()
    }

    private fun detachNow() {
        val window = nativeWindow
        try {
            if (fullscreen && window?.isValid == true) {
                window.hwnd().let(::leaveFullscreen)
            }
            window?.detachCallbacks()
            renderWindow.store(null)
            closeRenderer(contextLost = renderer?.isContextLost() == true)
        } finally {
            renderWindow.store(null)
            nativeWindow = null
            renderRequestState.store(0)
            detachAfterRender = false
            deferredRenderApi = null
        }
    }

    actual fun needRender(throttledToVsync: Boolean) {
        if (renderDelegateReference.load() == null) return
        val window = renderWindow.load() ?: return
        var previousState: Int
        while (true) {
            previousState = renderRequestState.load()
            val nextState =
                previousState or RENDER_REQUEST_PENDING or
                    if (throttledToVsync) 0 else RENDER_REQUEST_UNTHROTTLED
            if (renderRequestState.compareAndSet(previousState, nextState)) break
        }
        if (previousState and RENDER_REQUEST_PENDING == 0) window.requestRender()
    }

    @Deprecated(
        message = "Use needRender() instead",
        replaceWith = ReplaceWith("needRender()"),
    )
    actual fun needRedraw() = needRender()

    internal actual fun draw(canvas: Canvas) {
        val window = nativeWindow ?: return
        val (width, height) = window.clientSize
        if (width > 0 && height > 0) {
            renderDelegate?.onRender(canvas, width, height, currentNanoTime())
        }
    }

    /** True when [needRender] has requested a frame that has not been presented yet. */
    @InternalSkikoApi
    val hasPendingRender: Boolean
        get() = renderRequestState.load() and RENDER_REQUEST_PENDING != 0

    /** Current renderer and recovery counters for diagnostics and support tooling. */
    @InternalSkikoApi
    val diagnostics: WindowsSkiaLayerDiagnostics
        get() =
            WindowsSkiaLayerDiagnostics(
                renderApi = renderApi,
                rendererDescription = renderer?.description,
                deviceName = renderer?.deviceName,
                renderedFrameCount = renderedFrameCount,
                fallbackCount = fallbackCount,
                contextRecoveryCount = contextRecoveryCount,
                lastFailure = lastRenderFailure,
                isVsyncEnabled = properties.isVsyncEnabled,
                frameBuffering = properties.frameBuffering,
                effectiveFrameBufferCount = renderer?.effectiveFrameBufferCount,
                transparencyRequested = transparency,
                hasTransparentWindowBuffer = renderer?.transparencySupported == true,
                adapterPriority = properties.adapterPriority,
                gpuResourceCacheLimit = properties.gpuResourceCacheLimit,
                pixelGeometry = pixelGeometry,
                fpsAverage = fpsCounter?.average,
                fpsMinimum = fpsCounter?.min,
                fpsMaximum = fpsCounter?.max,
            )

    /** Renders one pending frame on the thread that owns the attached HWND. */
    @InternalSkikoApi
    fun render(force: Boolean = false): Boolean {
        if (!rendering.compareAndSet(expectedValue = false, newValue = true)) return false
        val requestState = renderRequestState.exchange(0)
        try {
            if (!force && requestState and RENDER_REQUEST_PENDING == 0) return false
            val window = nativeWindow ?: return false
            val delegate = renderDelegateReference.load() ?: return false
            val (width, height) = window.clientSize
            if (width <= 0 || height <= 0) return false

            val waitForVsync =
                requestState and RENDER_REQUEST_UNTHROTTLED == 0
            renderWithRecovery(width, height, waitForVsync) { canvas ->
                try {
                    delegate.onRender(canvas, width, height, currentNanoTime())
                } catch (failure: Throwable) {
                    throw RenderDelegateFailure(failure)
                }
            }
            return true
        } catch (failure: RenderDelegateFailure) {
            throw failure.cause ?: failure
        } finally {
            rendering.store(false)
            applyDeferredRendererActions()
        }
    }

    @InternalSkikoApi
    fun snapshot(width: Int, height: Int): Bitmap =
        rendererOperationWithRecovery { it.snapshot(width, height) }

    @InternalSkikoApi
    fun <T> withOpenGlContext(block: () -> T): T {
        check(renderer?.renderApi == GraphicsApi.OPENGL) {
            "External OpenGL access requires the OpenGL renderer"
        }
        return rendererOperationWithRecovery { activeRenderer ->
            activeRenderer.withExternalOpenGl {
                try {
                    block()
                } catch (failure: Throwable) {
                    throw RendererUserOperationFailure(failure)
                }
            }
        }
    }

    @InternalSkikoApi
    fun drawOpenGlTexture(textureId: Int, width: Int, height: Int, canvas: Canvas) {
        check(renderer?.renderApi == GraphicsApi.OPENGL) {
            "OpenGL textures require the OpenGL renderer"
        }
        rendererOperationWithRecovery { it.drawTexture(textureId, width, height, canvas) }
    }

    @InternalSkikoApi
    val rendererDescription: String
        get() = checkNotNull(renderer) { "SkiaLayer is not attached" }.description

    private fun applyDeferredRendererActions() {
        if (detachAfterRender) {
            detachNow()
            return
        }
        deferredRenderApi?.let { requestedApi ->
            deferredRenderApi = null
            replaceRenderer(requestedApi, countAsFallback = false)
            needRender(throttledToVsync = false)
        }
    }

    private fun recordWindowCallbackFailure(failure: Throwable) {
        lastRenderFailure = failure.message ?: failure::class.simpleName
        renderRequestState.store(0)
    }

    private fun nativeWindowDestroyed() {
        renderWindow.store(null)
        renderRequestState.store(0)
        savedWindowStyle = null
        savedWindowRect = null
        if (rendering.load()) {
            detachAfterRender = true
        } else {
            closeRenderer(contextLost = true, ignoreFailure = true)
            nativeWindow = null
            detachAfterRender = false
            deferredRenderApi = null
        }
    }

    private fun renderWithRecovery(
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ) {
        val previousContextRecoveryCount = contextRecoveryCount
        var activeRenderer = healthyRenderer()
        if (
            waitForVsync &&
                activeRenderer.renderApi in
                setOf(GraphicsApi.SOFTWARE_FAST, GraphicsApi.SOFTWARE_COMPAT) &&
                properties.isVsyncEnabled &&
                properties.isVsyncFramelimitFallbackEnabled
        ) {
            softwareFrameLimiter.awaitNextFrame(nativeWindow?.displayRefreshRate ?: 60f)
        }

        var attemptSameApiRecreation = contextRecoveryCount == previousContextRecoveryCount
        while (true) {
            try {
                renderFrame(activeRenderer, width, height, waitForVsync, block)
                return
            } catch (failure: RenderDelegateFailure) {
                throw failure
            } catch (failure: Throwable) {
                recoverRenderer(
                    failedRenderer = activeRenderer,
                    failure = failure,
                    attemptSameApiRecreation = attemptSameApiRecreation,
                )
                attemptSameApiRecreation = false
                activeRenderer = checkNotNull(renderer)
            }
        }
    }

    private fun renderFrame(
        renderer: WindowsLayerRenderer,
        width: Int,
        height: Int,
        waitForVsync: Boolean,
        block: (Canvas) -> Unit,
    ) {
        val isFirstFrame = !firstFrameRendered
        if (isFirstFrame) deviceAnalytics?.beforeFirstFrameRender()
        deviceAnalytics?.beforeFrameRender()
        renderer.render(width, height, waitForVsync, block)
        fpsCounter?.tick()
        renderedFrameCount += 1
        firstFrameRendered = true
        if (isFirstFrame) deviceAnalytics?.afterFirstFrameRender()
        deviceAnalytics?.afterFrameRender()
    }

    private inline fun <T> rendererOperationWithRecovery(
        operation: (WindowsLayerRenderer) -> T,
    ): T {
        val previousContextRecoveryCount = contextRecoveryCount
        var activeRenderer = healthyRenderer()
        var attemptSameApiRecreation = contextRecoveryCount == previousContextRecoveryCount
        while (true) {
            try {
                return operation(activeRenderer)
            } catch (failure: RendererUserOperationFailure) {
                throw failure.cause ?: failure
            } catch (failure: Throwable) {
                recoverRenderer(activeRenderer, failure, attemptSameApiRecreation)
                attemptSameApiRecreation = false
                activeRenderer = checkNotNull(renderer)
            }
        }
    }

    private fun healthyRenderer(): WindowsLayerRenderer {
        val activeRenderer = checkNotNull(renderer) { "SkiaLayer is not attached" }
        if (activeRenderer.isContextLost()) {
            recoverRenderer(
                activeRenderer,
                RenderException("Graphics context was lost"),
                attemptSameApiRecreation = true,
                knownContextLost = true,
            )
        }
        return checkNotNull(renderer)
    }

    private fun recoverRenderer(
        failedRenderer: WindowsLayerRenderer,
        failure: Throwable,
        attemptSameApiRecreation: Boolean,
        knownContextLost: Boolean? = null,
    ) {
        lastRenderFailure = failure.message ?: failure::class.simpleName
        val failedApi = failedRenderer.renderApi
        val contextLost = knownContextLost ?: failedRenderer.isContextLost()
        closeRenderer(contextLost = contextLost, ignoreFailure = true)

        if (
            failedApi in setOf(GraphicsApi.DIRECT3D, GraphicsApi.OPENGL) &&
                attemptSameApiRecreation
        ) {
            try {
                installRenderer(failedApi)
                contextRecoveryCount += 1
                return
            } catch (recreationFailure: Throwable) {
                lastRenderFailure = recreationFailure.message ?: recreationFailure::class.simpleName
            }
        }

        val fallbackQueue = SkikoProperties.fallbackRenderApiQueue(failedApi).drop(1)
        installFirstWorkingRenderer(fallbackQueue, countAsFallback = true)
    }

    private fun replaceRenderer(api: GraphicsApi, countAsFallback: Boolean) {
        closeRenderer(contextLost = renderer?.isContextLost() == true)
        installFirstWorkingRenderer(
            SkikoProperties.fallbackRenderApiQueue(api),
            countAsFallback = countAsFallback,
        )
    }

    private fun installFirstWorkingRenderer(
        candidates: List<GraphicsApi>,
        countAsFallback: Boolean,
    ) {
        var lastFailure: Throwable? = null
        candidates.forEachIndexed { index, api ->
            try {
                installRenderer(api)
                if (countAsFallback || index > 0) fallbackCount += 1
                return
            } catch (failure: Throwable) {
                lastFailure = failure
                lastRenderFailure = failure.message ?: failure::class.simpleName
            }
        }
        throw RenderException("Cannot initialize any Kotlin/Native Windows renderer", lastFailure)
    }

    private fun installRenderer(api: GraphicsApi) {
        val window = checkNotNull(nativeWindow)
        val nextRenderer =
            windowsLayerRendererFactoryOverride?.invoke(api, window, properties, pixelGeometry)
                ?: when (api) {
                    GraphicsApi.DIRECT3D ->
                        WindowsDirect3DRenderer(
                            window,
                            properties,
                            pixelGeometry,
                            transparency,
                        )
                    GraphicsApi.OPENGL ->
                        WindowsOpenGLRenderer(window, properties, pixelGeometry, transparency)
                    GraphicsApi.SOFTWARE_FAST,
                    GraphicsApi.SOFTWARE_COMPAT -> WindowsSoftwareRenderer(window, api, pixelGeometry)
                    else -> error("Kotlin/Native Windows does not support $api rendering")
                }
        val nextRendererAnalytics: SkiaLayerAnalytics.RendererAnalytics
        val nextDeviceAnalytics: SkiaLayerAnalytics.DeviceAnalytics
        try {
            nextRendererAnalytics = analytics.renderer(WindowsNativeSkikoVersion, hostOs, api)
            nextRendererAnalytics.init()
            nextRendererAnalytics.deviceChosen()
            nextDeviceAnalytics =
                analytics.device(WindowsNativeSkikoVersion, hostOs, api, nextRenderer.deviceName)
            nextDeviceAnalytics.init()
            nextDeviceAnalytics.contextInit()
        } catch (failure: Throwable) {
            try {
                nextRenderer.close(contextLost = nextRenderer.isContextLost())
            } catch (_: Throwable) {
                // Preserve the analytics failure while still making a best effort to release the renderer.
            }
            throw failure
        }

        renderer = nextRenderer
        rendererAnalytics = nextRendererAnalytics
        deviceAnalytics = nextDeviceAnalytics
        firstFrameRendered = false
        activeRenderApi = api
    }

    private fun closeRenderer(contextLost: Boolean, ignoreFailure: Boolean = false) {
        val oldRenderer = renderer ?: return
        renderer = null
        rendererAnalytics = null
        deviceAnalytics = null
        try {
            oldRenderer.close(contextLost)
        } catch (failure: Throwable) {
            if (!ignoreFailure) throw failure
            lastRenderFailure = failure.message ?: failure::class.simpleName
        }
    }

    private fun enterFullscreen(hwnd: HWND) = memScoped {
        val rect = alloc<RECT>()
        check(GetWindowRect(hwnd, rect.ptr) != 0) { "GetWindowRect failed" }
        savedWindowRect = WindowRect(rect.left, rect.top, rect.right, rect.bottom)
        val style = GetWindowLongPtrW(hwnd, GWL_STYLE)
        savedWindowStyle = style
        SetWindowLongPtrW(hwnd, GWL_STYLE, style and WS_OVERLAPPEDWINDOW.toLong().inv())

        val monitor = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST.convert())
        val monitorInfo = alloc<MONITORINFO>()
        monitorInfo.cbSize = sizeOf<MONITORINFO>().convert()
        check(GetMonitorInfoW(monitor, monitorInfo.ptr) != 0) { "GetMonitorInfoW failed" }
        val bounds = monitorInfo.rcMonitor
        SetWindowPos(
            hwnd,
            null,
            bounds.left,
            bounds.top,
            bounds.right - bounds.left,
            bounds.bottom - bounds.top,
            (SWP_NOOWNERZORDER or SWP_NOACTIVATE or SWP_FRAMECHANGED or SWP_SHOWWINDOW).convert(),
        )
    }

    private fun leaveFullscreen(hwnd: HWND) {
        val style = savedWindowStyle ?: return
        val rect = savedWindowRect ?: return
        SetWindowLongPtrW(hwnd, GWL_STYLE, style)
        SetWindowPos(
            hwnd,
            null,
            rect.left,
            rect.top,
            rect.right - rect.left,
            rect.bottom - rect.top,
            (SWP_NOOWNERZORDER or SWP_NOACTIVATE or SWP_FRAMECHANGED or SWP_SHOWWINDOW).convert(),
        )
        ShowWindow(hwnd, SW_RESTORE)
        savedWindowStyle = null
        savedWindowRect = null
    }
}

@InternalSkikoApi
data class WindowsSkiaLayerDiagnostics(
    val renderApi: GraphicsApi,
    val rendererDescription: String?,
    val deviceName: String?,
    val renderedFrameCount: Long,
    val fallbackCount: Int,
    val contextRecoveryCount: Int,
    val lastFailure: String?,
    val isVsyncEnabled: Boolean,
    val frameBuffering: FrameBuffering,
    val effectiveFrameBufferCount: Int?,
    val transparencyRequested: Boolean,
    val hasTransparentWindowBuffer: Boolean,
    val adapterPriority: GpuPriority,
    val gpuResourceCacheLimit: Long,
    val pixelGeometry: PixelGeometry,
    val fpsAverage: Int?,
    val fpsMinimum: Int?,
    val fpsMaximum: Int?,
)

private class RenderDelegateFailure(cause: Throwable) : RuntimeException(cause)
private class RendererUserOperationFailure(cause: Throwable) : RuntimeException(cause)

private const val RENDER_REQUEST_PENDING = 1
private const val RENDER_REQUEST_UNTHROTTLED = 1 shl 1

private const val WindowsNativeSkikoVersion = "Kotlin/Native"

actual val currentSystemTheme: SystemTheme
    get() {
        val themeValue = IntArray(1)
        val themeValueSize = IntArray(1) { Int.SIZE_BYTES }
        val status = themeValue.usePinned { valuePinned ->
            themeValueSize.usePinned { sizePinned ->
                RegGetValueW(
                    HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "AppsUseLightTheme",
                    RRF_RT_REG_DWORD.convert(),
                    null,
                    valuePinned.addressOf(0),
                    sizePinned.addressOf(0).reinterpret<UIntVar>(),
                )
            }
        }
        return if (status != 0) SystemTheme.UNKNOWN
        else if (themeValue[0] == 0) SystemTheme.DARK else SystemTheme.LIGHT
    }
