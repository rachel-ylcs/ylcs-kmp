@file:OptIn(ExperimentalAtomicApi::class)

package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_SystemCursor.*
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class SdlWindowScope(private val window: COpaquePointer, private val wakeEventType: UInt) : NativeWindowScope {
    private val windowId = RachelSDL_GetWindowID(window)
    private val active = AtomicBoolean(true)
    private val closing = AtomicBoolean(false)
    private val cursors = mutableMapOf<NativeWindowCursor, COpaquePointer>()
    private val trays = mutableSetOf<SdlNativeTray>()
    private var callbackFailure: Throwable? = null
    private var hitTest: SdlWindowHitTest? = null
    private var defaultDragArea: ((Float, Float) -> Boolean)? = null
    private val dragAreas = mutableMapOf<Any, (Float, Float) -> Boolean?>()
    private val dropTarget = SdlWindowDropTarget(window, ::onCallbackFailure)
    val shouldClose: Boolean get() = closing.load()

    override val displayScale: Float get() {
        checkHostThread()
        return RachelSDL_GetWindowDisplayScale(window).takeIf { it.isFinite() && it > 0f } ?: 1f
    }

    override val pixelDensity: Float get() {
        checkHostThread()
        return RachelSDL_GetWindowPixelDensity(window).takeIf { it.isFinite() && it > 0f } ?: 1f
    }

    override val pixelWidth: Int get() = memScoped {
        checkHostThread()
        val width = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, null)) { SDL_GetError()?.toKString() ?: "Unable to read window width" }
        width.value
    }

    override val pixelHeight: Int get() = memScoped {
        checkHostThread()
        val height = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, null, height.ptr)) { SDL_GetError()?.toKString() ?: "Unable to read window height" }
        height.value
    }

    override val state: NativeWindowState get() = memScoped {
        checkHostThread()
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        check(RachelSDL_GetWindowSize(window, width.ptr, height.ptr)) { SDL_GetError()?.toKString() ?: "Unable to read window size" }
        val x = alloc<IntVar>()
        val y = alloc<IntVar>()
        check(RachelSDL_GetWindowPosition(window, x.ptr, y.ptr)) { SDL_GetError()?.toKString() ?: "Unable to read window position" }
        val flags = RachelSDL_GetWindowFlags(window)
        NativeWindowState(
            title = RachelSDL_GetWindowTitle(window)?.toKString().orEmpty(),
            width = width.value,
            height = height.value,
            minimized = flags and SDL_WINDOW_MINIMIZED != 0uL,
            maximized = flags and SDL_WINDOW_MAXIMIZED != 0uL,
            visible = flags and SDL_WINDOW_HIDDEN == 0uL,
            alwaysOnTop = flags and SDL_WINDOW_ALWAYS_ON_TOP != 0uL,
            x = x.value,
            y = y.value,
            fullscreen = flags and SDL_WINDOW_FULLSCREEN != 0uL,
            resizable = flags and SDL_WINDOW_RESIZABLE != 0uL,
            clickThrough = RachelSDL_GetWindowClickThrough(window),
        )
    }

    override val displayBounds: NativeWindowBounds get() = memScoped {
        checkHostThread()
        val bounds = alloc<SDL_Rect>()
        check(RachelSDL_GetWindowDisplayBounds(window, bounds.ptr)) { SDL_GetError()?.toKString() ?: "Unable to read display bounds" }
        NativeWindowBounds(bounds.x, bounds.y, bounds.w, bounds.h)
    }

    override val windowHandle: Long? get() {
        checkHostThread()
        val properties = RachelSDL_GetWindowProperties(window)
        val win32 = SDL_GetPointerProperty(properties, SDL_PROP_WINDOW_WIN32_HWND_POINTER, null)
        if (win32 != null) return win32.rawValue.toLong()
        val cocoa = SDL_GetPointerProperty(properties, SDL_PROP_WINDOW_COCOA_WINDOW_POINTER, null)
        if (cocoa != null) return cocoa.rawValue.toLong()
        val x11 = SDL_GetNumberProperty(properties, SDL_PROP_WINDOW_X11_WINDOW_NUMBER, 0L)
        if (x11 != 0L) return x11
        return SDL_GetPointerProperty(properties, SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER, null)?.rawValue?.toLong()
    }

    override val displayHandle: Long? get() {
        checkHostThread()
        val properties = RachelSDL_GetWindowProperties(window)
        val display = SDL_GetPointerProperty(properties, SDL_PROP_WINDOW_X11_DISPLAY_POINTER, null)
            ?: SDL_GetPointerProperty(properties, SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER, null)
        return display?.rawValue?.toLong()
    }

    override val isPositionSupported: Boolean get() {
        checkHostThread()
        return SDL_GetCurrentVideoDriver()?.toKString() != "wayland"
    }

    override fun invalidate() {
        if (active.load()) RachelSDL_WakeEventLoop(wakeEventType, windowId)
    }

    override fun close() {
        if (active.load()) {
            closing.store(true)
            invalidate()
        }
    }

    override fun setCursor(cursor: NativeWindowCursor) {
        checkHostThread()
        val handle = cursors.getOrPut(cursor) {
            val kind = when (cursor) {
                NativeWindowCursor.DEFAULT -> SDL_SYSTEM_CURSOR_DEFAULT
                NativeWindowCursor.CROSSHAIR -> SDL_SYSTEM_CURSOR_CROSSHAIR
                NativeWindowCursor.TEXT -> SDL_SYSTEM_CURSOR_TEXT
                NativeWindowCursor.HAND -> SDL_SYSTEM_CURSOR_POINTER
                NativeWindowCursor.MOVE -> SDL_SYSTEM_CURSOR_MOVE
                NativeWindowCursor.NOT_ALLOWED -> SDL_SYSTEM_CURSOR_NOT_ALLOWED
                NativeWindowCursor.WAIT -> SDL_SYSTEM_CURSOR_WAIT
            }
            checkNotNull(RachelSDL_CreateCursor(kind)) { SDL_GetError()?.toKString() ?: "Unable to create cursor" }
        }
        check(RachelSDL_SetCursor(handle)) { SDL_GetError()?.toKString() ?: "Unable to set cursor" }
    }

    override fun setSize(width: Int, height: Int) {
        checkHostThread()
        require(width in 1 .. 16384 && height in 1 .. 16384 && width.toLong() * height <= 67108864)
        check(RachelSDL_SetWindowSize(window, width, height)) { SDL_GetError()?.toKString() ?: "Unable to resize window" }
    }

    override fun setPosition(x: Int, y: Int): Boolean {
        checkHostThread()
        if (!isPositionSupported) return false
        return RachelSDL_SetWindowPosition(window, x, y)
    }

    override fun setTitle(title: String) {
        checkHostThread()
        require(title.length <= 1024 && '\u0000' !in title) { "Invalid Native window title" }
        check(RachelSDL_SetWindowTitle(window, title)) { SDL_GetError()?.toKString() ?: "Unable to set window title" }
    }

    override fun setMinimumSize(width: Int, height: Int) {
        checkHostThread()
        require(width in 1 .. 16384 && height in 1 .. 16384 && width.toLong() * height <= 67108864)
        check(RachelSDL_SetWindowMinimumSize(window, width, height)) { SDL_GetError()?.toKString() ?: "Unable to set minimum window size" }
    }

    override fun setUndecorated(undecorated: Boolean) {
        checkHostThread()
        if (!undecorated && RachelSDL_GetWindowClickThrough(window)) setClickThrough(false)
        check(RachelSDL_SetWindowUndecorated(window, undecorated)) { SDL_GetError()?.toKString() ?: "Unable to change window decorations" }
    }

    override fun setFocusable(focusable: Boolean) {
        checkHostThread()
        if (SDL_GetCurrentVideoDriver()?.toKString() == "wayland") return
        check(RachelSDL_SetWindowFocusable(window, focusable)) { SDL_GetError()?.toKString() ?: "Unable to change window focusability" }
    }

    override fun setResizable(resizable: Boolean) {
        checkHostThread()
        check(RachelSDL_SetWindowResizable(window, resizable)) { SDL_GetError()?.toKString() ?: "Unable to change window resizability" }
    }

    override fun setClickThrough(enabled: Boolean) {
        checkHostThread()
        check(RachelSDL_SetWindowClickThrough(window, enabled)) { SDL_GetError()?.toKString() ?: "Unable to change window mouse passthrough" }
    }

    override fun setIcon(width: Int, height: Int, pixels: IntArray): Boolean {
        checkHostThread()
        require(width in 1 .. 1024 && height in 1 .. 1024 && pixels.size == width * height)
        return pixels.usePinned { RachelSDL_SetWindowIcon(window, width, height, it.addressOf(0)) }
    }

    override fun createTray(tooltip: String): NativeTray? {
        checkHostThread()
        return SdlNativeTray.create(tooltip) { trays.remove(it) }?.also { trays.add(it) }
    }

    override fun setDragArea(hitTest: ((Float, Float) -> Boolean)?) {
        checkHostThread()
        defaultDragArea = hitTest
        updateDragAreas()
    }

    override fun addDragArea(hitTest: (Float, Float) -> Boolean?): AutoCloseable {
        checkHostThread()
        val registration = Any()
        dragAreas[registration] = hitTest
        try { updateDragAreas() }
        catch (failure: Throwable) {
            dragAreas.remove(registration)
            throw failure
        }
        return object : AutoCloseable {
            override fun close() {
                if (!active.load()) return
                checkHostThread()
                if (dragAreas.remove(registration) != null) updateDragAreas()
            }
        }
    }

    private fun isDragArea(x: Float, y: Float): Boolean {
        var draggable = defaultDragArea?.invoke(x, y) == true
        for (area in dragAreas.values) {
            when (area(x, y)) {
                false -> return false
                true -> draggable = true
                null -> Unit
            }
        }
        return draggable
    }

    override fun startDrag(): Boolean {
        checkHostThread()
        return RachelSDL_StartDrag(window)
    }

    private fun updateDragAreas() {
        val current = this.hitTest
        if (defaultDragArea == null && dragAreas.isEmpty()) {
            current?.dispose()
            this.hitTest = null
        }
        else if (current == null) this.hitTest = SdlWindowHitTest(window, ::isDragArea, ::onCallbackFailure)
    }

    override fun setVisible(visible: Boolean) {
        checkHostThread()
        check(RachelSDL_SetWindowVisible(window, visible)) { SDL_GetError()?.toKString() ?: "Unable to change window visibility" }
    }

    override fun setAlwaysOnTop(alwaysOnTop: Boolean) {
        checkHostThread()
        check(RachelSDL_SetWindowAlwaysOnTop(window, alwaysOnTop)) { SDL_GetError()?.toKString() ?: "Unable to change window topmost state" }
    }

    override fun setMinimized(minimized: Boolean) {
        checkHostThread()
        val flags = RachelSDL_GetWindowFlags(window)
        if (minimized) {
            if (flags and SDL_WINDOW_MINIMIZED == 0uL) check(RachelSDL_MinimizeWindow(window)) { SDL_GetError()?.toKString() ?: "Unable to minimize window" }
        }
        else if (flags and SDL_WINDOW_MINIMIZED != 0uL) {
            check(RachelSDL_RestoreWindow(window)) { SDL_GetError()?.toKString() ?: "Unable to restore window" }
            if (flags and SDL_WINDOW_MAXIMIZED != 0uL) check(RachelSDL_MaximizeWindow(window)) { SDL_GetError()?.toKString() ?: "Unable to restore maximized window" }
        }
    }

    override fun setMaximized(maximized: Boolean) {
        checkHostThread()
        val flags = RachelSDL_GetWindowFlags(window)
        if (maximized) {
            if (flags and SDL_WINDOW_MAXIMIZED == 0uL || flags and SDL_WINDOW_MINIMIZED != 0uL) check(RachelSDL_MaximizeWindow(window)) { SDL_GetError()?.toKString() ?: "Unable to maximize window" }
        }
        else if (flags and SDL_WINDOW_MAXIMIZED != 0uL) check(RachelSDL_RestoreWindow(window)) { SDL_GetError()?.toKString() ?: "Unable to restore window" }
    }

    override fun setFullscreen(fullscreen: Boolean) {
        checkHostThread()
        check(RachelSDL_SetFullscreen(window, fullscreen)) { SDL_GetError()?.toKString() ?: "Unable to change window fullscreen state" }
    }

    override fun setDropTarget(target: NativeWindowDropTarget?) {
        checkHostThread()
        dropTarget.target = target
    }

    private fun onCallbackFailure(failure: Throwable) {
        if (callbackFailure == null) callbackFailure = failure
        close()
    }

    fun rethrowCallbackFailure() {
        callbackFailure?.let { throw it }
    }

    override fun startTextInput() {
        checkHostThread()
        check(RachelSDL_StartTextInput(window)) { SDL_GetError()?.toKString() ?: "Unable to start text input" }
    }

    override fun stopTextInput() {
        checkHostThread()
        check(RachelSDL_StopTextInput(window)) { SDL_GetError()?.toKString() ?: "Unable to stop text input" }
    }

    override fun setTextInputArea(x: Int, y: Int, width: Int, height: Int) {
        checkHostThread()
        require(width >= 0 && height >= 0) { "Text input area has a negative size" }
        check(RachelSDL_SetTextInputArea(window, x, y, width, height)) { SDL_GetError()?.toKString() ?: "Unable to set text input area" }
    }

    fun dispose() {
        active.store(false)
        trays.toList().forEach { it.close() }
        hitTest?.dispose()
        hitTest = null
        defaultDragArea = null
        dragAreas.clear()
        dropTarget.dispose()
        cursors.values.forEach(::RachelSDL_DestroyCursor)
        cursors.clear()
    }

    private fun checkHostThread() {
        check(active.load() && SDL_IsMainThread()) { "Native window controls require an active host on the main thread" }
    }
}
