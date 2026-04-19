package love.yinlin.tpl

import androidx.compose.runtime.Stable
import love.yinlin.data.music.PlatformMusicInfo

@Stable
object KugouMusicAPI : PlatformMusicAPI {
    override suspend fun search(keyword: String): List<PlatformMusicInfo>? {
        return null
    }

    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? {
        return null
    }
}