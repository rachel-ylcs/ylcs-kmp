package love.yinlin.compose.ui

expect class PAGFile : PAGComposition {
    companion object {
        val MaxSupportedTagLevel: Int
        fun load(path: String): PAGFile
        fun load(bytes: ByteArray): PAGFile
        suspend fun loadAsync(path: String): PAGFile
        suspend fun loadAsync(bytes: ByteArray): PAGFile
    }

    val tagLevel: Int
    val numTexts: Int
    val numImages: Int
    val numVideos: Int
    val path: String
    fun replaceImage(index: Int, image: PAGImage)
    fun replaceImageByName(layerName: String, image: PAGImage)
    fun getEditableIndices(layerType: PAGLayerType): IntArray
    var timeStretchMode: PAGTimeStretchMode
    fun setDuration(duration: Long)
    fun copyOriginal(): PAGFile
}