package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix

expect class PAGPlayer {
    var surface: PAGSurface?
    var composition: PAGComposition?
    var videoEnabled: Boolean
    var cacheEnabled: Boolean
    var useDiskCache: Boolean
    var cacheScale: Float
    var maxFrameRate: Float
    var scaleMode: PAGScaleMode
    var matrix: Matrix
    val duration: Long
    var progress: Double
    val currentFrame: Long
    fun prepare()
    fun flush()
    fun flushAndFenceSync(syncArray: LongArray)
    fun waitSync(sync: Long)
    fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean
    val renderingTime: Long
    val imageDecodingTime: Long
    val presentingTime: Long
    val graphicsMemory: Long
    fun close()
}