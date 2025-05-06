package love.yinlin.api

import androidx.compose.runtime.Stable

@Stable
data class QQMusic(
    val id: String, // 音乐 ID
    val name: String, // 名称
    val singer: String, // 歌手
    val time: String, // 时长
    val pic: String, // 封面
    val lyrics: String, // 歌词
    val oggUrl: String, // OGG 下载链接
)

object QQMusicAPI {

}