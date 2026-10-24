package org.jetbrains.skiko.window

import org.jetbrains.skia.Canvas

import kotlinx.cinterop.COpaquePointer

internal interface WindowRenderer {
    val description: String
    fun render(width: Int, height: Int): Boolean
    fun close()
}

internal expect fun gpuWindowFlags(): ULong
internal expect fun createGpuRenderer(window: COpaquePointer, draw: (Canvas, Int, Int) -> Unit): WindowRenderer