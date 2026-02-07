package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.compatible.await
import love.yinlin.extension.asIntArray
import love.yinlin.platform.unsupportedPlatform
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual class PAGFile(override val delegate: PlatformPAGFile) : PAGComposition(delegate) {
    actual companion object {
        actual val MaxSupportedTagLevel: Int get() = PlatformPAGFile.maxSupportedTagLevel()
        actual fun load(path: String): PAGFile = unsupportedPlatform()
        actual fun load(bytes: ByteArray): PAGFile = unsupportedPlatform()
        actual suspend fun loadAsync(path: String): PAGFile = unsupportedPlatform()
        @OptIn(CompatibleRachelApi::class, ExperimentalWasmJsInterop::class)
        actual suspend fun loadAsync(bytes: ByteArray): PAGFile = PAGFile(PlatformPAGFile.loadFromBuffer(ByteArrayCompatible(bytes).asInt8Array.buffer).await())
    }

    actual val tagLevel: Int get() = delegate.tagLevel()
    actual val numTexts: Int get() = delegate.numTexts()
    actual val numImages: Int get() = delegate.numImages()
    actual val numVideos: Int get() = delegate.numVideos()
    actual val path: String get() = unsupportedPlatform()
    actual fun replaceImage(index: Int, image: PAGImage) = delegate.replaceImage(index, image.delegate)
    actual fun replaceImageByName(layerName: String, image: PAGImage) { unsupportedPlatform() }
    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun getEditableIndices(layerType: PAGLayerType): IntArray = delegate.getEditableIndices(layerType.ordinal).asIntArray
    actual var timeStretchMode: PAGTimeStretchMode get() = PAGTimeStretchMode.entries[delegate.timeStretchMode()]
        set(value) { delegate.setTimeStretchMode(value.ordinal) }
    actual fun setDuration(duration: Long) = delegate.setDuration(duration.toDouble())
    actual fun copyOriginal(): PAGFile = PAGFile(delegate.copyOriginal())
}