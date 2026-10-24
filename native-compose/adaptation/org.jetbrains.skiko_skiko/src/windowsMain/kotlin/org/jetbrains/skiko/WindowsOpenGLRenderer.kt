@file:OptIn(ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.*
import org.jetbrains.skia.*
import platform.opengl32.GL_RENDERER
import platform.opengl32.glFlush
import platform.opengl32.glGetString
import platform.windows.*

internal class WindowsOpenGLRenderer(
    private val window: WindowsNativeWindow,
    private val properties: SkiaLayerProperties,
    private val pixelGeometry: PixelGeometry,
    private val transparencyRequested: Boolean,
) : WindowsLayerRenderer {
    private val wglDevice = run {
        loadOpenGLLibrary()
        createDeviceContext(window, transparencyRequested)
    }
    private val deviceContext = wglDevice.deviceContext
    private val previousOpenGlContext = wglGetCurrentContext()
    private val previousDeviceContext = wglGetCurrentDC()

    override val renderApi: GraphicsApi = GraphicsApi.OPENGL
    override val effectiveFrameBufferCount: Int = if (wglDevice.doubleBuffered) 2 else 1
    override val transparencySupported: Boolean get() = transparencyRequested && wglDevice.hasTransparentWindowBuffer

    private val openGlContext = wglCreateContext(deviceContext) ?: run {
        ReleaseDC(window.hwnd(), deviceContext)
        throw RenderException("Could not create a WGL context (Win32 error ${GetLastError()})")
    }

    private val openGlInterface: GLAssembledInterface
    private val directContext: DirectContext
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var closed = false
    private var contextFailed = false
    private val frameLimiter = WindowsFrameLimiter()
    private var swapIntervalFunction: CPointer<CFunction<(Int) -> Int>>? = null
    private var graphicsResetStatusFunction: CPointer<CFunction<() -> UInt>>? = null
    private var appliedSwapInterval: Int? = null

    override var deviceName: String? = null
        private set

    override var description: String = "Skia Ganesh/WGL"
        private set

    init {
        var createdInterface: GLAssembledInterface? = null
        var createdContext: DirectContext? = null
        try {
            makeCurrent()
            swapIntervalFunction = validWglFunctionPointer(wglGetProcAddress("wglSwapIntervalEXT"))?.reinterpret()
            graphicsResetStatusFunction = (validWglFunctionPointer(wglGetProcAddress("glGetGraphicsResetStatus")) ?: validWglFunctionPointer(wglGetProcAddress("glGetGraphicsResetStatusARB")))?.reinterpret()
            val glInterface = GLAssembledInterface.createFromNativePointers(ctxPtr = NativePtr.NULL, fPtr = windowsOpenGlResolver)
            createdInterface = glInterface
            val context = DirectContext.makeGLWithInterface(glInterface)
            createdContext = context
            if (properties.gpuResourceCacheLimit >= 0L) {
                context.resourceCacheLimit = properties.gpuResourceCacheLimit
            }
            deviceName = currentOpenGlRendererName()
            if (!isOpenGlAdapterSupported(deviceName)) {
                throw RenderException("OpenGL adapter is not supported: $deviceName")
            }
            if (transparencyRequested && !enableTransparentComposition(window)) {
                throw RenderException("The Win32 compositor could not enable transparency for the WGL window")
            }
            description = if (deviceName.isNullOrBlank()) "Skia Ganesh/WGL" else "Skia Ganesh/WGL ($deviceName)"
        }
        catch (failure: Throwable) {
            contextFailed = true
            releaseResources(contextLost = true, propagateFailure = false, context = createdContext, glInterface = createdInterface)
            throw failure as? RenderException ?: RenderException("Could not initialize the WGL renderer", failure)
        }
        openGlInterface = checkNotNull(createdInterface)
        directContext = checkNotNull(createdContext)
    }

    override fun render(width: Int, height: Int, waitForVsync: Boolean, block: (Canvas) -> Unit) {
        makeCurrent()
        checkForGraphicsReset()
        val shouldWaitForVsync = waitForVsync && properties.isVsyncEnabled
        val platformVsyncEnabled = wglDevice.doubleBuffered && configureSwapInterval(shouldWaitForVsync)
        ensureSurface(width, height)
        val skiaSurface = checkNotNull(surface)
        skiaSurface.canvas.clear(Color.TRANSPARENT)
        block(skiaSurface.canvas)
        skiaSurface.flushAndSubmit()
        checkForGraphicsReset()
        if (wglDevice.doubleBuffered) {
            if (SwapBuffers(deviceContext) == 0) {
                contextFailed = true
                throw RenderException("SwapBuffers failed (Win32 error ${GetLastError()})")
            }
        }
        else glFlush()

        if (shouldWaitForVsync && !platformVsyncEnabled) {
            val dwmWaitedForComposition = DwmFlush() == 0
            if (!dwmWaitedForComposition && properties.isVsyncFramelimitFallbackEnabled) {
                frameLimiter.awaitNextFrame(window.displayRefreshRate)
            }
        }
    }

    private fun configureSwapInterval(enabled: Boolean): Boolean {
        val function = swapIntervalFunction ?: return false
        val interval = if (enabled) 1 else 0
        if (appliedSwapInterval == interval) return true
        return if (function(interval) != 0) {
            appliedSwapInterval = interval
            true
        }
        else {
            // Some drivers expose WGL_EXT_swap_control but reject it for a particular drawable.
            swapIntervalFunction = null
            appliedSwapInterval = null
            false
        }
    }

    private fun makeCurrent() {
        check(!closed) { "Renderer is closed" }
        if (wglMakeCurrent(deviceContext, openGlContext) == 0) {
            contextFailed = true
            throw RenderException("wglMakeCurrent failed (Win32 error ${GetLastError()})")
        }
    }

    private fun checkForGraphicsReset() {
        val resetStatus = graphicsResetStatusFunction?.invoke() ?: GL_NO_ERROR
        if (resetStatus != GL_NO_ERROR) {
            contextFailed = true
            throw RenderException("The WGL context was reset (OpenGL status 0x${resetStatus.toString(16)})")
        }
    }

    override fun <T> withExternalOpenGl(block: () -> T): T {
        makeCurrent()
        surface?.flushAndSubmit()
        return try {
            block()
        }
        finally {
            directContext.resetGLAll()
        }
    }

    override fun drawTexture(textureId: Int, width: Int, height: Int, canvas: Canvas) {
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
        }
        catch (failure: Throwable) {
            texture.close()
            throw failure
        }
        try {
            canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()), null)
        }
        finally {
            image.close()
            texture.close()
        }
    }

    override fun snapshot(width: Int, height: Int): Bitmap {
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
            stencilBits = wglDevice.stencilBits,
            fbId = 0,
            fbFormat = FramebufferFormat.GR_GL_RGBA8,
        )
        surface = Surface.makeFromBackendRenderTarget(
            context = directContext,
            rt = checkNotNull(renderTarget),
            origin = SurfaceOrigin.BOTTOM_LEFT,
            colorFormat = SurfaceColorFormat.RGBA_8888,
            colorSpace = ColorSpace.sRGB,
            surfaceProps = SurfaceProps(pixelGeometry = pixelGeometry),
        ) ?: throw RenderException("Skia could not wrap the WGL framebuffer")
    }

    override fun isContextLost(): Boolean {
        if (closed || contextFailed || !window.isValid) return true
        if (wglMakeCurrent(deviceContext, openGlContext) == 0) {
            contextFailed = true
            return true
        }
        val resetStatus = graphicsResetStatusFunction?.invoke() ?: GL_NO_ERROR
        if (resetStatus != GL_NO_ERROR) contextFailed = true
        return contextFailed
    }

    override fun close(contextLost: Boolean) {
        releaseResources(contextLost = contextLost, propagateFailure = true, context = directContext, glInterface = openGlInterface)
    }

    private fun releaseResources(contextLost: Boolean, propagateFailure: Boolean, context: DirectContext?, glInterface: GLAssembledInterface?) {
        if (closed) return
        val contextIsCurrent = !contextLost && !contextFailed && window.isValid && wglMakeCurrent(deviceContext, openGlContext) != 0
        if (!contextIsCurrent) contextFailed = true
        closed = true

        var cleanupFailure: Throwable? = null

        if (!contextIsCurrent) cleanupFailure = releaseRendererResource(cleanupFailure) { context?.abandon() }
        val oldSurface = surface
        surface = null
        cleanupFailure = releaseRendererResource(cleanupFailure) { oldSurface?.close() }
        val oldRenderTarget = renderTarget
        renderTarget = null
        cleanupFailure = releaseRendererResource(cleanupFailure) { oldRenderTarget?.close() }
        cleanupFailure = releaseRendererResource(cleanupFailure) { context?.close() }
        cleanupFailure = releaseRendererResource(cleanupFailure) { glInterface?.close() }

        cleanupFailure = releaseRendererResource(cleanupFailure) {
            val restored = if (previousOpenGlContext != null) wglMakeCurrent(previousDeviceContext, previousOpenGlContext) else wglMakeCurrent(null, null)
            if (restored == 0) throw RenderException("Could not restore the previous WGL context (Win32 error ${GetLastError()})")
        }
        cleanupFailure = releaseRendererResource(cleanupFailure) {
            if (wglDeleteContext(openGlContext) == 0) throw RenderException("wglDeleteContext failed (Win32 error ${GetLastError()})")
        }
        cleanupFailure = releaseRendererResource(cleanupFailure) {
            if (ReleaseDC(window.hwnd(), deviceContext) == 0) throw RenderException("ReleaseDC failed (Win32 error ${GetLastError()})")
        }

        if (propagateFailure) {
            cleanupFailure?.let { throw RenderException("Could not fully release the WGL renderer", it) }
        }
    }

    private fun isOpenGlAdapterSupported(name: String?): Boolean {
        if (name == null || SkikoProperties.allowSoftwareOpenGlAdapter) return true
        if (name.contains("GDI Generic", ignoreCase = true)) return false
        return isVideoCardSupported(GraphicsApi.OPENGL, hostOs, name)
    }

    private companion object {
        const val GL_TEXTURE_2D = 0x0DE1
        const val GL_RGBA8 = 0x8058
        const val GL_NO_ERROR = 0u
    }
}

private fun enableTransparentComposition(window: WindowsNativeWindow): Boolean = memScoped {
    val compositionEnabled = alloc<IntVar>()
    if (DwmIsCompositionEnabled(compositionEnabled.ptr) < 0 || compositionEnabled.value == 0) {
        return false
    }

    val blurRegion = CreateRectRgn(0, 0, -1, -1) ?: return false
    try {
        val blurBehind = alloc<DWM_BLURBEHIND>()
        blurBehind.dwFlags = (DWM_BB_ENABLE or DWM_BB_BLURREGION).convert()
        blurBehind.fEnable = 1
        blurBehind.hRgnBlur = blurRegion
        blurBehind.fTransitionOnMaximized = 0
        DwmEnableBlurBehindWindow(window.hwnd(), blurBehind.ptr) >= 0
    }
    finally {
        DeleteObject(blurRegion)
    }
}

private fun currentOpenGlRendererName(): String? = glGetString(GL_RENDERER.toUInt())?.reinterpret<ByteVar>()?.toKString()

private val windowsOpenGlResolver = staticCFunction(::resolveWindowsOpenGlProc).rawValue

private fun resolveWindowsOpenGlProc(
    @Suppress("UNUSED_PARAMETER")
    context: COpaquePointer?,
    name: CPointer<ByteVar>?,
): COpaquePointer? {
    val functionName = name?.toKString() ?: return null
    validWglFunctionPointer(wglGetProcAddress(functionName))?.let { return it.reinterpret() }

    val openGlModule = GetModuleHandleW("opengl32.dll") ?: return null
    return GetProcAddress(openGlModule, functionName)?.reinterpret()
}

private fun validWglFunctionPointer(pointer: CPointer<out CPointed>?): CPointer<out CPointed>? {
    val address = pointer?.rawValue?.toLong() ?: 0L
    return pointer.takeIf { address !in INVALID_WGL_ADDRESSES }
}

private val INVALID_WGL_ADDRESSES: Set<Long> = [0L, 1L, 2L, 3L, -1L]