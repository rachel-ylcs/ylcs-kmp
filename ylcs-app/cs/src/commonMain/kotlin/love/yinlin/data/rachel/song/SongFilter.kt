package love.yinlin.data.rachel.song

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class SongFilter(
    val key: String?, // 关键词
    val singer: String?, // 歌手
    val lyricist: String?, // 作词
    val composer: String?, // 作曲
    val album: String?, // 专辑
    val useAnimation: Boolean, // 动画
    val useVideo: Boolean, // 视频
    val useRhyme: Boolean, // 音游
    val useAccompaniment: Boolean, // 伴奏
)