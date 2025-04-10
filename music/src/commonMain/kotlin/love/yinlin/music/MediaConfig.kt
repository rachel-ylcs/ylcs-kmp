package love.yinlin.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class MediaConfig(
    val version: String, // 版本
    val author: String, // 作者
    val id: String, // ID
    val name: String, // 名称
    val singer: String, // 演唱
    val lyricist: String, // 作词
    val composer: String, // 作曲
    val album: String, // 专辑
    val chorus: List<Int>?, // 副歌点
)