package love.yinlin.screen.world.single.rhyme.data

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.extension.translate
import love.yinlin.screen.world.single.rhyme.RhymeConfig

@Stable
@Suppress("ConstPropertyName")
object Tracks {
    // 音阶表
    val Scales = intArrayOf(4, 5, 6, 1, 2, 3, 7)
    // 数量
    val Size = Scales.size
    // 顶点偏移比率
    const val VerticesTopRatio = 0.2f
    // 虚拟顶点
    const val VirtualTopHeight = VerticesTopRatio * RhymeConfig.HEIGHT
    // 虚拟画布宽度
    const val VirutalWidth = RhymeConfig.WIDTH
    // 虚拟画布高度
    const val VirtualHeight = RhymeConfig.HEIGHT + VirtualTopHeight
    // 顶点
    val Vertices = Offset(VirutalWidth / 2, 0f)
    // 轨道宽
    val Width = VirutalWidth / Size

    // 轨道
    val mTracks = buildList {
        var start = 0f
        repeat(Size) { index ->
            val left = Offset(start, VirtualHeight)
            val right = left.translate(x = Width)
            add(Track(
                index = index,
                scale = Scales[index],
                left = left,
                right = right
            ))
            start += Width
        }
    }

    operator fun get(index: Int): Track = mTracks[index]
    fun first(): Track = mTracks.first()
    fun last(): Track = mTracks.last()
    val lastIndex: Int = mTracks.lastIndex

    inline fun <R> map(block: (Track) -> R): List<R> = mTracks.map(block)

    inline fun foreach(block: (Track) -> Unit) {
        for (track in mTracks) block(track)
    }

    inline fun foreachIndexed(block: (Int, Track) -> Unit) {
        for (index in 0 ..< Size) block(index, mTracks[index])
    }
}