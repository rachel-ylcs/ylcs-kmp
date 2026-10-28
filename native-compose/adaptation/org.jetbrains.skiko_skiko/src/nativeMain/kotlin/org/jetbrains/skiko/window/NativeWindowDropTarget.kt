package org.jetbrains.skiko.window

fun interface NativeWindowDropTarget {
    fun accept(types: Int, x: Float, y: Float): Int

    @Suppress("ConstPropertyName")
    companion object {
        const val File = 1
        const val Text = 2
    }
}
