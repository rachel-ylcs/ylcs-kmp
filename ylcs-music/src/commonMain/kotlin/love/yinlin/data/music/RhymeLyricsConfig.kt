package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 音符操作
@Serializable
@Stable
sealed interface RhymeAction {
    // 单音符
    @Serializable
    @Stable
    @SerialName("Note")
    data class Note(
        val ch: Char, // 字符
        // 起始偏移 (起始时间 = 当前句的起始时间 + 偏移)
        val start: Int,
        // 终止偏移 (终止时间 = 当前句的终止时间 + 偏移)
        val end: Int,
        // 音阶
        // (低音 1 - 7 -> 15 - 21)
        // (中音 1 - 7 -> 1 - 7)
        // (高音 1 - 7 -> 8 - 14)
        val scale: Byte,
    ) : RhymeAction

    // 连音符
    @Serializable
    @Stable
    @SerialName("Slur")
    data class Slur(
        val ch: Char, // 字符
        val start: Int, // 起始偏移
        val end: Int, // 终止偏移
        val scale: List<Byte>, // 连音阶组 (每个音阶均分字符总时长)
    ) : RhymeAction
}

// 歌词行
@Serializable
@Stable
data class RhymeLine(
    // 行内容
    // 仅作显示, 所以允许包含空格或符号等, 但每个音符操作的字符只能是单个字符
    val text: String,
    val start: Long, // 起始时间
    val end: Long, // 终止时间
    val theme: List<RhymeAction>, // 主旋律
    val chord: List<RhymeAction>? = null, // 和弦
)

// 歌词
@Serializable
@Stable
data class RhymeLyricsConfig(
    val id: String, // ID, 同 config 中的 ID
    val duration: Long, // 时长
    val lyrics: List<RhymeLine>, // 歌词
)