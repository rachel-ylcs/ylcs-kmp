package org.jetbrains.skiko.window

interface NativeWindowScope {
    val displayScale: Float
    val pixelDensity: Float
    val pixelWidth: Int
    val pixelHeight: Int
    val state: NativeWindowState
    val displayBounds: NativeWindowBounds
    /** Borrowed HWND, NSWindow pointer, X11 Window ID or wl_surface pointer; valid until close. */
    val windowHandle: Long?
    /** Borrowed X11 Display or wl_display pointer on Linux, null on other platforms. */
    val displayHandle: Long?
    /** Wayland compositors do not expose global positions or allow clients to move their windows. */
    val isPositionSupported: Boolean
    fun invalidate()
    fun close()
    fun setSize(width: Int, height: Int)
    /** Requests a move in SDL desktop coordinates. False means the window system rejected it. */
    fun setPosition(x: Int, y: Int): Boolean
    fun setTitle(title: String)
    fun setMinimumSize(width: Int, height: Int)
    fun setUndecorated(undecorated: Boolean)
    fun setIcon(width: Int, height: Int, pixels: IntArray): Boolean
    fun createTray(tooltip: String = ""): NativeTray?
    fun setDragArea(hitTest: ((Float, Float) -> Boolean)?)
    fun setVisible(visible: Boolean)
    fun setAlwaysOnTop(alwaysOnTop: Boolean)
    fun setMinimized(minimized: Boolean)
    fun setMaximized(maximized: Boolean)
    fun setFullscreen(fullscreen: Boolean)
    fun setCursor(cursor: NativeWindowCursor)
    fun setDropTarget(target: NativeWindowDropTarget?)
    fun startTextInput()
    fun stopTextInput()
    fun setTextInputArea(x: Int, y: Int, width: Int, height: Int)
}
