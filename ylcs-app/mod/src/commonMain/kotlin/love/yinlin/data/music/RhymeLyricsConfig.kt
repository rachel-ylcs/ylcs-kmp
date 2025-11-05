package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 音符操作
@Stable
@Serializable
sealed interface RhymeAction {
    // 字符
    val ch: String
    // 终止偏移 (终止时间 = 当前句的终止时间 + 偏移)
    val end: Int

    // 单音符
    @Stable
    @Serializable
    @SerialName("Note")
    data class Note(
        override val ch: String,
        override val end: Int,
        // 音阶
        // (低音 1 - 7 -> 15 - 21)
        // (中音 1 - 7 -> 1 - 7)
        // (高音 1 - 7 -> 8 - 14)
        val scale: Byte,
    ) : RhymeAction

    // 连音符
    @Stable
    @Serializable
    @SerialName("Slur")
    data class Slur(
        override val ch: String,
        override val end: Int,
        val scale: List<Byte>, // 连音阶组 (每个音阶均分字符总时长)
    ) : RhymeAction
}

// 歌词行
@Stable
@Serializable
data class RhymeLine(
    // 行内容
    // 仅作显示, 所以允许包含空格或符号等, 但每个音符操作的字符只能是单个字符
    val text: String,
    val start: Long, // 起始时间
    val theme: List<RhymeAction>, // 主旋律
)

// 副歌段
@Stable
@Serializable
data class Chorus(
    val start: Long,
    val end: Long
)

// 歌词
@Stable
@Serializable
data class RhymeLyricsConfig(
    val id: String, // ID, 同 config 中的 ID
    val duration: Long, // 时长
    val chorus: List<Chorus>, // 副歌点
    val lyrics: List<RhymeLine>, // 歌词
)