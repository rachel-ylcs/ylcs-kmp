package org.jetbrains.skiko.window

interface NativeTray {
    val isActive: Boolean
    fun setIcon(width: Int, height: Int, pixels: IntArray)
    fun setTooltip(tooltip: String)
    fun close()
}
