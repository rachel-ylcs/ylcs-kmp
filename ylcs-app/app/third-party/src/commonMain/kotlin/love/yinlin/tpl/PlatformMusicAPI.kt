package love.yinlin.tpl

import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType

interface PlatformMusicAPI {
    suspend fun search(keyword: String): List<PlatformMusicInfo>?
    suspend fun parseLink(link: String): List<PlatformMusicInfo>?

    companion object {
        fun build(type: PlatformMusicType): PlatformMusicAPI = when (type) {
            PlatformMusicType.QQMusic -> QQMusicAPI
            PlatformMusicType.NetEaseCloud -> NetEaseCloudAPI
            PlatformMusicType.Kugou -> KugouMusicAPI
        }
    }
}