package love.yinlin.compose.ui

expect class PAGSurface {
    companion object {
        fun makeOffscreen(width: Int, height: Int): PAGSurface
    }

    val width: Int
    val height: Int
    fun updateSize()
    fun clearAll()
    fun close()
}