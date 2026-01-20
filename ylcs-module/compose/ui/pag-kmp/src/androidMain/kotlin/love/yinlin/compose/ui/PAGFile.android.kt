package love.yinlin.compose.ui

actual class PAGFile(override val delegate: PlatformPAGFile) : PAGComposition(delegate) {
    actual companion object {
        actual val MaxSupportedTagLevel: Int get() = PlatformPAGFile.MaxSupportedTagLevel()
        actual fun load(path: String): PAGFile = PAGFile(PlatformPAGFile.Load(path))
        actual fun load(bytes: ByteArray): PAGFile = PAGFile(PlatformPAGFile.Load(bytes))
        actual suspend fun loadAsync(path: String): PAGFile = load(path)
        actual suspend fun loadAsync(bytes: ByteArray): PAGFile = load(bytes)
    }

    actual val tagLevel: Int get() = delegate.tagLevel()
    actual val numTexts: Int get() = delegate.numTexts()
    actual val numImages: Int get() = delegate.numImages()
    actual val numVideos: Int get() = delegate.numVideos()
    actual val path: String get() = delegate.path()
    actual fun replaceImage(index: Int, image: PAGImage) = delegate.replaceImage(index, image.delegate)
    actual fun replaceImageByName(layerName: String, image: PAGImage) = delegate.replaceImageByName(layerName, image.delegate)
    actual fun getEditableIndices(layerType: PAGLayerType): IntArray = delegate.getEditableIndices(layerType.ordinal)
    actual var timeStretchMode: PAGTimeStretchMode get() = PAGTimeStretchMode.entries[delegate.timeStretchMode()]
        set(value) { delegate.setTimeStretchMode(value.ordinal) }
    actual fun setDuration(duration: Long) = delegate.setDuration(duration)
    actual fun copyOriginal(): PAGFile = PAGFile(delegate.copyOriginal())
}