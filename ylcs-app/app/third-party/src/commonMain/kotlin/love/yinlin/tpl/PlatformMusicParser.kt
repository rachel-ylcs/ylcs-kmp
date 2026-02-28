package love.yinlin.tpl

import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType

interface PlatformMusicParser {
    suspend fun parseLink(link: String): List<PlatformMusicInfo>?

    companion object {
        fun build(type: PlatformMusicType): PlatformMusicParser = when (type) {
            PlatformMusicType.QQMusic -> QQMusicAPI
            PlatformMusicType.NetEaseCloud -> NetEaseCloudAPI
            PlatformMusicType.Kugou -> KugouMusicAPI
        }
    }
}