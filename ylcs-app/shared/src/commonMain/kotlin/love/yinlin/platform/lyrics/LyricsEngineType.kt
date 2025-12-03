package love.yinlin.platform.lyrics

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.mod.ModResourceType

@Stable
@Serializable
enum class LyricsEngineType(
    val key: String,
    val title: String,
    val description: String,
    val resType: ModResourceType
) {
    Line("line", "逐行歌词", "逐行显示的歌词", ModResourceType.LineLyrics),
    Rhyme("rhyme", "琴韵歌词", "逐字显示的动态歌词", ModResourceType.Rhyme);
}