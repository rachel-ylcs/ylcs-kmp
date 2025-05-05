package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Transient
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class MusicInfo(
    val version: String, // 版本
    val author: String, // 作者
    val id: String, // ID
    val name: String, // 名称
    val singer: String, // 演唱
    val lyricist: String, // 作词
    val composer: String, // 作曲
    val album: String, // 专辑
    val chorus: List<Long>?, // 副歌点
    @Transient val modification: Int = 0 // 修改标记
)