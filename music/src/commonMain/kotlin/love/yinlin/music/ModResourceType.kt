package love.yinlin.music

enum class ModResourceType(
    val id: Int,
    val description: String,
    val default: String? = null
) {
    Config(0, "媒体配置", "config"),
    Audio(10, "音源"),
    Record(20, "封面", "record"),
    Background(21, "壁纸", "background"),
    Animation(22, "动画", "animation"),
    LineLyrics(30, "逐行歌词"),
    Video(40, "视频");

    companion object {
        fun fromInt(id: Int): ModResourceType? = when (id) {
            0 -> Config
            10 -> Audio
            20 -> Record
            21 -> Background
            22 -> Animation
            30 -> LineLyrics
            40 -> Video
            else -> null
        }
    }
}