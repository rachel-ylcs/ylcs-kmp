@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual class PAGFile(override val delegate: PlatformPAGFile) : PAGComposition(delegate) {
    actual companion object {
        actual val MaxSupportedTagLevel: Int get() = PlatformPAGFile.MaxSupportedTagLevel().toInt()
        actual fun load(path: String): PAGFile = PAGFile(PlatformPAGFile.Load(path)!!)
        actual fun load(bytes: ByteArray): PAGFile = bytes.usePinned { pinned ->
            PAGFile(PlatformPAGFile.Load(pinned.addressOf(0), bytes.size.toULong())!!)
        }
        actual suspend fun loadAsync(path: String): PAGFile = load(path)
        actual suspend fun loadAsync(bytes: ByteArray): PAGFile = load(bytes)
    }

    actual val tagLevel: Int get() = delegate.tagLevel().toInt()
    actual val numTexts: Int get() = delegate.numTexts()
    actual val numImages: Int get() = delegate.numImages()
    actual val numVideos: Int get() = delegate.numVideos()
    actual val path: String get() = delegate.path()!!
    actual fun replaceImage(index: Int, image: PAGImage) { delegate.replaceImage(index, image.delegate) }
    actual fun replaceImageByName(layerName: String, image: PAGImage) { delegate.replaceImageByName(layerName, image.delegate) }
    actual fun getEditableIndices(layerType: PAGLayerType): IntArray = delegate.getEditableIndices(layerType.ordinal.asPAGLayerType)!!.map { it.toInt() }.toIntArray()
    actual var timeStretchMode: PAGTimeStretchMode get() = PAGTimeStretchMode.entries[delegate.timeStretchMode().toInt()]
        set(value) { delegate.seTimeStretchMode(value.ordinal.toUInt()) } // 逆天笔误
    actual fun setDuration(duration: Long) { delegate.setDuration(duration) }
    actual fun copyOriginal(): PAGFile = PAGFile(delegate.copyOriginal()!!)
}