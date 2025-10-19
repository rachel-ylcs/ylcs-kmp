package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class MusicResourceType(
    val id: Int,
    val description: String,
    val uniqueName: String? = null,
    val defaultName: String? = uniqueName
) {
    Config(
        id = 0,
        description = "媒体配置",
        uniqueName = "config"
    ),
    Audio(
        id = 10,
        description = "音源",
        defaultName = "flac"
    ),
    Record(
        id = 20,
        description = "封面",
        uniqueName = "record"
    ),
    Background(
        id = 21,
        description = "壁纸",
        uniqueName = "background"
    ),
    Animation(
        id = 22,
        description = "动画",
        uniqueName = "animation"
    ),
    LineLyrics(
        id = 30,
        description = "逐行歌词",
        defaultName = "lrc"
    ),
    Video(
        id = 40,
        description = "视频",
        defaultName = "pv"
    ),
    Rhyme(
        id = 50,
        description = "音游配置",
        uniqueName = "rhyme"
    );

    companion object {
        fun fromInt(id: Int): MusicResourceType? = when (id) {
            0 -> Config
            10 -> Audio
            20 -> Record
            21 -> Background
            22 -> Animation
            30 -> LineLyrics
            40 -> Video
            50 -> Rhyme
            else -> null
        }
    }

    val default: MusicResource get() = MusicResource(id, defaultName ?: "")
}