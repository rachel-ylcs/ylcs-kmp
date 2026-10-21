package love.yinlin.compose.image.data

import androidx.compose.runtime.Stable

@Stable
internal class GifFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val duration: Int,
    val disposal: Int,
    val transparent: Int,
    val interlaced: Boolean,
    val palette: IntArray,
    val codeSize: Int,
    val dataOffset: Int,
)
