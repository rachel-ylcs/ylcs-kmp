package love.yinlin.compose.ui

actual class PAGFile(override val delegate: PlatformPAGFile) : PAGComposition(delegate) {
    actual companion object {
        actual val MaxSupportedTagLevel: Int get() = PlatformPAGFile.MaxSupportedTagLevel.toInt()
        actual fun load(path: String): PAGFile = PAGFile(PlatformPAGFile.loadFromPath(path))
        actual fun load(bytes: ByteArray): PAGFile = PAGFile(PlatformPAGFile.loadFromBytes(bytes))
        actual suspend fun loadAsync(path: String): PAGFile = load(path)
        actual suspend fun loadAsync(bytes: ByteArray): PAGFile = load(bytes)
    }

    actual val tagLevel: Int by delegate::tagLevel
    actual val numTexts: Int by delegate::numTexts
    actual val numImages: Int by delegate::numImages
    actual val numVideos: Int by delegate::numVideos
    actual val path: String by delegate::path
    actual fun replaceImage(index: Int, image: PAGImage) = delegate.replaceImage(index, image.delegate)
    actual fun replaceImageByName(layerName: String, image: PAGImage) = delegate.replaceImageByName(layerName, image.delegate)
    actual fun getEditableIndices(layerType: PAGLayerType): IntArray = delegate.getEditableIndices(layerType.ordinal)
    actual var timeStretchMode: PAGTimeStretchMode get() = PAGTimeStretchMode.entries[delegate.timeStretchMode]
        set(value) { delegate.timeStretchMode = value.ordinal }
    actual fun setDuration(duration: Long) = delegate.setDuration(duration)
    actual fun copyOriginal(): PAGFile = PAGFile(delegate.copyOriginal())
}