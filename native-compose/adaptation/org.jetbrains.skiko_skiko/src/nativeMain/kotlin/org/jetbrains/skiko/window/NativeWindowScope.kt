package org.jetbrains.skiko.window

interface NativeWindowScope {
    val displayScale: Float
    val pixelDensity: Float
    val pixelWidth: Int
    val pixelHeight: Int
    val state: NativeWindowState
    fun invalidate()
    fun close()
    fun setSize(width: Int, height: Int)
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
    fun setCursor(cursor: NativeWindowCursor)
    fun setDropTarget(target: NativeWindowDropTarget?)
    fun startTextInput()
    fun stopTextInput()
    fun setTextInputArea(x: Int, y: Int, width: Int, height: Int)
}
