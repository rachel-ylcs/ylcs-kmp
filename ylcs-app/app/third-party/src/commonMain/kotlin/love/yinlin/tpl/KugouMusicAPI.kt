package love.yinlin.tpl

import androidx.compose.runtime.Stable
import love.yinlin.data.music.PlatformMusicInfo

@Stable
object KugouMusicAPI : PlatformMusicParser {
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = null
}