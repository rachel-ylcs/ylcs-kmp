package org.jetbrains.skiko.window

data class NativeWindowState(
    val title: String,
    val width: Int,
    val height: Int,
    val minimized: Boolean = false,
    val maximized: Boolean = false,
    val visible: Boolean = true,
    val alwaysOnTop: Boolean = false,
)
