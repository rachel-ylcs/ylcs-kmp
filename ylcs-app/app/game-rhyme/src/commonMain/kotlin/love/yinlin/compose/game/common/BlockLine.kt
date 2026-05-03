package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable

@Stable
data class BlockLine(
    val lineCount: Int, // 总行数
    val index: Int, // 行索引
    val blockCount: Int, // 音符数
    val firstRawIndex: Int, // 第一个音符的原始索引
    val lastRawIndex: Int, // 最后一个音符的原始索引
    val startDirection: BlockDirection, // 开始方向
    val endDirection: BlockDirection?, // 结束方向
    val text: String, // 歌词文本
    val lineStart: Long, // 行开始时间
    val lineEnd: Long, // 行结束时间
)