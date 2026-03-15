package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.serialization.Transient
import kotlinx.serialization.Serializable
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.data.mod.ModResourceType
import love.yinlin.fs.File

@Stable
@Serializable
data class MusicInfo(
    val version: String, // 版本
    val author: String, // 作者
    override val id: String, // ID
    override val name: String, // 名称
    override val singer: String, // 演唱
    override val lyricist: String, // 作词
    override val composer: String, // 作曲
    override val album: String, // 专辑
    val chorus: List<Long>?, // 副歌点
    @Transient val modification: Int = 0 // 修改标记
) : MediaInfo {
    fun path(root: File, type: ModResourceType? = null) = if (type == null) File(root, id) else File(root, id, type.filename)
}