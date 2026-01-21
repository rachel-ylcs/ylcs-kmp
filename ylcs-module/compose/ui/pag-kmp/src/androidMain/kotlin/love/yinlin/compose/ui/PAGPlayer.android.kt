package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asAndroidMatrix
import love.yinlin.compose.graphics.asComposeMatrix

actual class PAGPlayer(private val delegate: PlatformPAGPlayer) {
    actual var surface: PAGSurface? get() = delegate.surface?.let(::PAGSurface)
        set(value) { delegate.surface = value?.delegate }
    actual var composition: PAGComposition? get() = delegate.composition?.let(::PAGComposition)
        set(value) { delegate.composition = value?.delegate }
    actual var videoEnabled: Boolean get() = delegate.videoEnabled()
        set(value) { delegate.setVideoEnabled(value) }
    actual var cacheEnabled: Boolean get() = delegate.cacheEnabled()
        set(value) { delegate.setCacheEnabled(value) }
    actual var useDiskCache: Boolean get() = delegate.useDiskCache()
        set(value) { delegate.setUseDiskCache(value) }
    actual var cacheScale: Float get() = delegate.cacheScale()
        set(value) { delegate.setCacheScale(value) }
    actual var maxFrameRate: Float get() = delegate.maxFrameRate()
        set(value) { delegate.setMaxFrameRate(value) }
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asAndroidMatrix()) }
    actual val duration: Long get() = delegate.duration()
    actual var progress: Double by delegate::progress
    actual val currentFrame: Long get() = delegate.currentFrame()
    actual fun prepare() { delegate.prepare() }
    actual fun flush() { delegate.flush() }
    actual fun flushAndFenceSync(syncArray: LongArray) { delegate.flushAndFenceSync(syncArray) }
    actual fun waitSync(sync: Long) { delegate.waitSync(sync) }
    actual fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean =
        delegate.hitTestPoint(layer.delegate, x, y, pixelHitTest)
    actual val renderingTime: Long = 0L
    actual val imageDecodingTime: Long = 0L
    actual val presentingTime: Long = 0L
    actual val graphicsMemory: Long = 0L
    actual fun close() = delegate.release()
}