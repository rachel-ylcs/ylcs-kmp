package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaInfo

@Stable
interface MediaMetadataFetcher {
    /**
     * 开启音频焦点
     */
    val audioFocus: Boolean

    /**
     * 更新间隔
     */
    val interval: Long

    /**
     * 提取音频
     */
    fun extractAudioUri(id: String): String?

    /**
     * 提取封面
     */
    fun extractCoverUri(id: String): String?

    /**
     * 提取元信息
     */
    fun extractMetadata(id: String): MediaInfo?

    /**
     * Android 后台服务名
     */
    val androidMusicServiceComponentName: Pair<String, String>
}