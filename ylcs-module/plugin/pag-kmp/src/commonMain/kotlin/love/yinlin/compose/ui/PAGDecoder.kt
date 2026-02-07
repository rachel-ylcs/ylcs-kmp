package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect class PAGDecoder {
    companion object {
        fun makeFrom(composition: PAGComposition, maxFrameRate: Float = 30f, scale: Float = 1f): PAGDecoder
    }

    val width: Int
    val height: Int
    val numFrames: Int
    val frameRate: Float
    fun checkFrameRate(index: Int): Boolean
    fun close()
}