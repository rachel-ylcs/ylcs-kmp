@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asCGAffineTransform
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.platform.unsupportedPlatform
import platform.CoreGraphics.CGPointMake

actual class PAGPlayer(private val delegate: PlatformPAGPlayer) {
    actual var surface: PAGSurface? get() = delegate.getSurface()?.let(::PAGSurface)
        set(value) { value?.delegate?.let { delegate.setSurface(it) } }
    actual var composition: PAGComposition? get() = delegate.getComposition()?.let(::PAGComposition)
        set(value) { value?.delegate?.let { delegate.setComposition(it) } }
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
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode().toInt()]
        set(value) { delegate.setScaleMode(value.ordinal.toUInt()) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asCGAffineTransform()) }
    actual val duration: Long get() = delegate.duration()
    actual var progress: Double get() = delegate.getProgress()
        set(value) { delegate.setProgress(value) }
    actual val currentFrame: Long get() = delegate.currentFrame()
    actual fun prepare() { delegate.prepare() }
    actual fun flush() { delegate.flush() }
    actual fun flushAndFenceSync(syncArray: LongArray) { unsupportedPlatform() }
    actual fun waitSync(sync: Long) { unsupportedPlatform() }
    actual fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean =
        delegate.hitTestPoint(layer.delegate, CGPointMake(x.toDouble(), y.toDouble()), pixelHitTest)
    actual val renderingTime: Long = 0L
    actual val imageDecodingTime: Long = 0L
    actual val presentingTime: Long = 0L
    actual val graphicsMemory: Long = 0L
    actual fun close() { }
}