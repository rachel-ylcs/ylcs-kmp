package org.jetbrains.skiko

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import org.jetbrains.skia.*
import org.jetbrains.skiko.redrawer.Redrawer
import platform.AppKit.*
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

/**
 * SkiaLayer implementation for macOS.
 * Supports [GraphicsApi.METAL] and [GraphicsApi.OPENGL].
 */
@OptIn(BetaInteropApi::class)
actual open class SkiaLayer {
    private var macosComponent: MacosSkiaLayerComponent? = null
    private var macosRenderer: MacosNativeRenderer? = null
    private var macosRenderPending = false
    private var macosRenderWaitsForVsync = true

    fun isShowing(): Boolean {
        return true
    }

    /**
     * Graphics API used by the native macOS renderer.
     *
     * Metal remains the default, while OpenGL is available for systems where Metal is unavailable
     * (for example, virtualized macOS environments).
     */
    actual var renderApi: GraphicsApi = GraphicsApi.METAL
        set(value) {
            if (value != GraphicsApi.METAL && value != GraphicsApi.OPENGL) {
                throw IllegalArgumentException("$field is not supported in macOS")
            }
            if (macosComponent != null && value != field) {
                throw IllegalStateException("The macOS native renderer cannot be changed after attach")
            }
            field = value
        }

    /**
     * The scale factor of [NSWindow]
     * https://developer.apple.com/documentation/appkit/nswindow/1419459-backingscalefactor
     */
    actual val contentScale: Float
        get() =
            macosComponent?.contentScale
                ?: if (this::nsView.isInitialized) {
                    nsView.window?.backingScaleFactor?.toFloat() ?: 1.0f
                } else {
                    1.0f
                }

    /**
     * Fullscreen is not supported
     */
    actual var fullscreen: Boolean
        get() = macosComponent?.fullscreen ?: false
        set(value) {
            val component = macosComponent
            if (component != null) {
                component.fullscreen = value
            } else if (value) {
                throw IllegalArgumentException("fullscreen unsupported")
            }
        }

    /**
     * Underlying [NSView]
     */
    lateinit var nsView: NSView
        private set

    actual val component: Any?
        get() = macosComponent?.windowHandle ?: if (this::nsView.isInitialized) nsView else null

    /**
     * Implements rendering logic and events processing.
     */
    actual var renderDelegate: SkikoRenderDelegate? = null
        set(value) {
            field = value
            if (value != null && macosComponent != null) needRender(throttledToVsync = false)
        }

    internal var redrawer: Redrawer? = null

    /**
     * Created/updated by recording in [update].
     * It's used as a source for drawing on canvas in [draw].
     */
    private var picture: PictureHolder? = null
    private val pictureRecorder = PictureRecorder()

    private val nsViewObserver = object : NSObject() {
        @ObjCAction
        fun frameDidChange(notification: NSNotification) {
            redrawer?.syncBoundsFromPlatformComponent()
            redrawer?.renderImmediately()
        }

        @ObjCAction
        fun windowDidChangeBackingProperties(notification: NSNotification) {
            redrawer?.syncBoundsFromPlatformComponent()
            redrawer?.renderImmediately()
        }

        fun addObserver() {
            val center = NSNotificationCenter.defaultCenter()
            center.addObserver(
                    observer = this,
                    selector = NSSelectorFromString("frameDidChange:"),
                    name = NSViewFrameDidChangeNotification,
                    `object` = nsView,
                )
            center.addObserver(
                observer = this,
                selector = NSSelectorFromString("windowDidChangeBackingProperties:"),
                name = NSWindowDidChangeBackingPropertiesNotification,
                `object` = nsView.window,
            )
        }

        fun removeObserver() {
            val center = NSNotificationCenter.defaultCenter()
            center.removeObserver(this)
        }
    }

    /**
     * @param container - should be an instance of [NSView]
     */
    actual fun attachTo(container: Any) {
        if (container is MacosSkiaLayerComponent) {
            check(macosComponent == null && redrawer == null) { "SkiaLayer is already attached" }
            macosComponent = container
            try {
                macosRenderer =
                    when (renderApi) {
                        GraphicsApi.OPENGL -> MacosOpenGLRenderer(container)
                        GraphicsApi.METAL -> MacosMetalRenderer(container)
                        else -> error("Unsupported macOS native renderer: $renderApi")
                    }
            } catch (failure: Throwable) {
                macosComponent = null
                throw failure
            }
            if (renderDelegate != null) needRender(throttledToVsync = false)
            return
        }
        check(!this::nsView.isInitialized) { "Already attached to another NSView" }
        check(container is NSView) { "container should be an instance of NSView" }
        nsView = container
        nsView.postsFrameChangedNotifications = true
        nsViewObserver.addObserver()
        redrawer = createNativeRedrawer(this, renderApi).apply {
            syncBoundsFromPlatformComponent()
            needRender()
        }
    }

    actual fun detach() {
        macosRenderer?.close()
        macosRenderer = null
        macosComponent = null
        macosRenderPending = false
        macosRenderWaitsForVsync = true
        if (redrawer == null) return
        nsViewObserver.removeObserver()
        redrawer?.dispose()
        redrawer = null
    }

    /**
     * Schedules a frame to an appropriate moment.
     */
    actual fun needRender(throttledToVsync: Boolean) {
        val component = macosComponent
        if (component != null) {
            if (renderDelegate == null) return
            macosRenderWaitsForVsync =
                if (macosRenderPending) macosRenderWaitsForVsync && throttledToVsync
                else throttledToVsync
            macosRenderPending = true
            component.requestRender()
        } else {
            redrawer?.needRender(throttledToVsync)
        }
    }

    @InternalSkikoApi
    fun render(force: Boolean = false): Boolean {
        val component = macosComponent
        if (component == null) {
            needRender(throttledToVsync = !force)
            return true
        }
        if (!force && !macosRenderPending) return false
        val renderer = macosRenderer ?: return false
        val delegate = renderDelegate ?: return false
        val width = component.drawableWidth.coerceAtLeast(0)
        val height = component.drawableHeight.coerceAtLeast(0)
        if (width <= 0 || height <= 0) return false

        val waitForVsync = if (macosRenderPending) macosRenderWaitsForVsync else true
        macosRenderPending = false
        macosRenderWaitsForVsync = true
        val rendered = renderer.render(width, height, waitForVsync) { canvas ->
            delegate.onRender(canvas, width, height, currentNanoTime())
        }
        if (!rendered) {
            macosRenderPending = true
            macosRenderWaitsForVsync = waitForVsync
            component.requestRender()
        }
        return rendered
    }

    @InternalSkikoApi
    val rendererDescription: String
        get() = macosRenderer?.description ?: "Skiko ${renderApi.name}"

    @InternalSkikoApi
    fun snapshot(width: Int, height: Int): Bitmap {
        val renderer = macosRenderer
        if (renderer is MacosOpenGLRenderer) return renderer.snapshot(width, height)

        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val bitmap = Bitmap()
        check(bitmap.allocN32Pixels(safeWidth, safeHeight)) {
            "Could not allocate macOS capture bitmap"
        }
        bitmap.erase(0)
        val canvas = Canvas(bitmap)
        try {
            renderDelegate?.onRender(canvas, safeWidth, safeHeight, 0L)
        } finally {
            canvas.close()
        }
        bitmap.notifyPixelsChanged()
        return bitmap
    }

    @InternalSkikoApi
    fun <T> withOpenGlContext(block: () -> T): T =
        checkNotNull(macosRenderer as? MacosOpenGLRenderer) {
            "The macOS native renderer is not OpenGL"
        }.withExternalOpenGl(block)

    @InternalSkikoApi
    fun drawOpenGlTexture(textureId: Int, width: Int, height: Int, canvas: Canvas) {
        checkNotNull(macosRenderer as? MacosOpenGLRenderer) {
            "The macOS native renderer is not OpenGL"
        }.drawTexture(textureId, width, height, canvas)
    }

    @Deprecated(
        message = "Use needRender() instead",
        replaceWith = ReplaceWith("needRender()")
    )
    actual fun needRedraw() = needRender()

    /**
     * Updates the [picture] according to current [nanoTime]
     */
    internal fun update(nanoTime: Long) {
        val width = nsView.frame.useContents { size.width }
        val height = nsView.frame.useContents { size.height }

        val pictureWidth = (width * contentScale).coerceAtLeast(0.0)
        val pictureHeight = (height * contentScale).coerceAtLeast(0.0)

        val canvas = pictureRecorder.beginRecording(0f, 0f, pictureWidth.toFloat(), pictureHeight.toFloat()).apply {
            clear(Color.WHITE)
        }
        renderDelegate?.onRender(canvas, pictureWidth.toInt(), pictureHeight.toInt(), nanoTime)

        val picture = pictureRecorder.finishRecordingAsPicture()
        this.picture = PictureHolder(picture, pictureWidth.toInt(), pictureHeight.toInt())
    }

    internal actual fun draw(canvas: Canvas) {
        picture?.also {
            canvas.drawPicture(it.instance)
        }
    }

    actual val pixelGeometry: PixelGeometry
        get() = PixelGeometry.UNKNOWN

    private fun createDrawScope() = LayerDrawScope(
        pixelGeometry = pixelGeometry,
        layerWidth = nsView.frame.useContents { size.width },
        layerHeight = nsView.frame.useContents { size.height },
        scale = contentScale
    )

    internal fun inDrawScope(block: LayerDrawScope.() -> Unit) {
        with(createDrawScope()) {
            block()
        }
    }
}
