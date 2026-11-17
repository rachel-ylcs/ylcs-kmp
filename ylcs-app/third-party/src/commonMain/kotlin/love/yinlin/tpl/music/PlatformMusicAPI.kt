package love.yinlin.tpl.music

import kotlinx.coroutines.flow.Flow
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.uri.Uri

interface PlatformMusicRecognizer {
    val type: PlatformMusicType
    fun matchesLink(uri: Uri): Boolean
    fun matchesTrackId(id: String): Boolean = false
}

interface PlatformMusicSearch {
    /** 逐批返回歌曲摘要。请求失败通过异常传递，正常结束时允许没有结果。 */
    fun search(keyword: String): Flow<List<PlatformMusicInfo>>
    fun parseLink(uri: Uri): Flow<List<PlatformMusicInfo>>
    fun findTrack(id: String): Flow<List<PlatformMusicInfo>>
}

interface PlatformMusicDownload {
    /** 仅在导入时解析资源。缺少歌词时允许返回空字符串。 */
    suspend fun resolveDownload(track: PlatformMusicInfo): PlatformMusicResources?

    fun modId(track: PlatformMusicInfo): String
}

interface PlatformMusicAPI : PlatformMusicRecognizer, PlatformMusicSearch, PlatformMusicDownload {
    override fun modId(track: PlatformMusicInfo): String = type.prefix + track.id
}
