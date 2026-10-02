package love.yinlin.data.bilibili

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import love.yinlin.data.information.UnifiedMessage

@Stable
@Serializable
sealed interface Bilibili : UnifiedMessage {
    // 图文
    @Stable
    @Serializable
    data class Draw(
        override val id: String,
        override val user: BilibiliUserInfo,
        override val time: LocalDateTime,
        override val location: String,
        override val title: String,
        override val content: String,
        override val data: BilibiliData,
        override val medias: List<BilibiliMedia>,
    ) : Bilibili {
        constructor(
            raw: BilibiliDynamic,
            title: String,
            content: String,
            medias: List<BilibiliMedia>
        ) : this(raw.id, raw.user, raw.time, raw.location, title, content, raw.data, medias)
    }

    // 视频
    @Stable
    @Serializable
    data class Video(
        override val id: String,
        override val user: BilibiliUserInfo,
        override val time: LocalDateTime,
        override val location: String,
        override val title: String,
        override val content: String,
        override val data: BilibiliData,
        override val medias: List<BilibiliMedia>,
    ) : Bilibili {
        constructor(
            raw: BilibiliDynamic,
            title: String,
            content: String,
            medias: List<BilibiliMedia>,
            danmakuNum: String,
            playNum: String
        ) : this(raw.id, raw.user, raw.time, raw.location, title, content, raw.data.copy(danmakuNum = danmakuNum, playNum = playNum), medias)
    }

    override fun compareTo(other: UnifiedMessage): Int = this.time.compareTo(other.time)
}