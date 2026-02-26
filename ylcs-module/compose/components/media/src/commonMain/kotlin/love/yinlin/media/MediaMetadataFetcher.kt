package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.compose.data.media.MediaInfo

@Stable
interface MediaMetadataFetcher<Info : MediaInfo> {
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
    val Info.audioUri: String

    /**
     * 提取封面
     */
    val Info.coverUri: String

    /**
     * Android 后台服务名
     */
    val androidMusicServiceComponentName: Pair<String, String>
}