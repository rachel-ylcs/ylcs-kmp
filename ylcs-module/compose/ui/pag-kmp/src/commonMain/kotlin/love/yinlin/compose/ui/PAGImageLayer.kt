package love.yinlin.compose.ui

expect class PAGImageLayer : PAGLayer {
    companion object {
        fun make(width: Int, height: Int, duration: Long): PAGImageLayer
    }

    val contentDuration: Long
    fun replaceImage(image: PAGImage)
    fun setImage(image: PAGImage)
    fun layerTimeToContent(time: Long): Long
    fun contentTimeToLayer(time: Long): Long
}