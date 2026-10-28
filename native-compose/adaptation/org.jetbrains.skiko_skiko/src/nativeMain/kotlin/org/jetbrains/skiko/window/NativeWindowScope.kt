package org.jetbrains.skiko.window

interface NativeWindowScope {
    val displayScale: Float
    val pixelDensity: Float
    val pixelWidth: Int
    val pixelHeight: Int
    fun invalidate()
    fun close()
    fun setSize(width: Int, height: Int)
    fun setCursor(cursor: NativeWindowCursor)
    fun setDropTarget(target: NativeWindowDropTarget?)
    fun startTextInput()
    fun stopTextInput()
    fun setTextInputArea(x: Int, y: Int, width: Int, height: Int)
}
