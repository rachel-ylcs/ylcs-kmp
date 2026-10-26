package org.jetbrains.skiko.window

/**
 * Borrowed window controls, valid only during [NativeWindow.open].
 * [invalidate] and [close] may be called from other threads; other operations require the host thread.
 */
interface NativeWindowScope {
    val displayScale: Float
    val pixelDensity: Float
    /** Current drawable dimensions in physical pixels; valid on the host thread. */
    val pixelWidth: Int
    val pixelHeight: Int
    fun invalidate()
    fun close()
    fun setSize(width: Int, height: Int)
    fun setCursor(cursor: NativeWindowCursor)
    fun startTextInput()
    fun stopTextInput()
    fun setTextInputArea(x: Int, y: Int, width: Int, height: Int)
}
