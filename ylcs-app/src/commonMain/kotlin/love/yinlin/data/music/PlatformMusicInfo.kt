package love.yinlin.data.music

import androidx.compose.runtime.Stable

@Stable
data class PlatformMusicInfo(
    val id: String, // ID
    val name: String, // 名称
    val singer: String, // 歌手
    val time: String, // 时长
    val pic: String, // 封面
    val audioUrl: String, // 音频链接
    val lyrics: String, // 歌词
)