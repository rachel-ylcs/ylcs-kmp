package love.yinlin.data.mod

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class ModResourceType(
    val type: String,
    val description: String,
    val base: Boolean = false
) {
    Config(
        type = "config",
        description = "配置",
        base = true,
    ),
    Audio(
        type = "audio",
        description = "音源",
        base = true,
    ),
    Record(
        type = "record",
        description = "封面",
        base = true,
    ),
    Background(
        type = "background",
        description = "壁纸",
        base = true,
    ),
    LineLyrics(
        type = "lyrics",
        description = "逐行歌词",
        base = true,
    ),
    Animation(
        type = "animation",
        description = "动画",
    ),
    Video(
        type = "video",
        description = "视频",
    ),
    Rhyme(
        type = "rhyme",
        description = "音游配置"
    );

    val filename: String get() = "$type.$RES_EXT"

    companion object {
        const val MOD_EXT = "rachel"
        const val RES_EXT = "${MOD_EXT}res"

        val BASE = entries.filter { it.base }
        val ALL = entries.toList()

        fun fromType(type: String): ModResourceType? = when (type) {
            Config.type -> Config
            Audio.type -> Audio
            Record.type -> Record
            Background.type -> Background
            LineLyrics.type -> LineLyrics
            Animation.type -> Animation
            Video.type -> Video
            Rhyme.type -> Rhyme
            else -> null
        }
    }
}