package org.jetbrains.skiko.window

interface NativeWindowScope {
    val displayScale: Float
    val pixelDensity: Float
    val pixelWidth: Int
    val pixelHeight: Int
    val state: NativeWindowState
    val displayBounds: NativeWindowBounds
    val windowHandle: Long?
    val displayHandle: Long?
    val isPositionSupported: Boolean
    fun invalidate()
    fun close()
    fun setSize(width: Int, height: Int)
    fun setPosition(x: Int, y: Int): Boolean
    fun setTitle(title: String)
    fun setMinimumSize(width: Int, height: Int)
    fun setUndecorated(undecorated: Boolean)
    fun setFocusable(focusable: Boolean)
    fun setResizable(resizable: Boolean)
    fun setClickThrough(enabled: Boolean)
    fun setIcon(width: Int, height: Int, pixels: IntArray): Boolean
    fun createTray(tooltip: String = ""): NativeTray?
    fun setDragArea(hitTest: ((Float, Float) -> Boolean)?)
    fun addDragArea(hitTest: (Float, Float) -> Boolean?): AutoCloseable
    fun startDrag(): Boolean
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
