package love.yinlin.platform.lyrics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import love.yinlin.data.mod.ModResourceType

@Stable
@Serializable
enum class LyricsEngineType(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val description: String,
    val resType: ModResourceType
) {
    Line(
        key = "line",
        title = "逐行歌词",
        icon = Icons.Outlined.Lyrics,
        description = "逐行显示的歌词",
        resType = ModResourceType.LineLyrics
    ),
    Rhyme(
        key = "rhyme",
        title = "琴韵歌词",
        icon = Icons.Outlined.Animation,
        description = "逐字显示的动态歌词",
        resType = ModResourceType.Rhyme
    );

    companion object {
        val DefaultOrder = listOf(Rhyme, Line)
    }
}