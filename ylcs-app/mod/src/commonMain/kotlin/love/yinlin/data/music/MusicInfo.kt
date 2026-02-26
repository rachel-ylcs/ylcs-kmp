package love.yinlin.data.music

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import kotlinx.serialization.Transient
import kotlinx.serialization.Serializable
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.data.mod.ModResourceType

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
    fun path(root: Path, type: ModResourceType? = null) = if (type == null) Path(root, id) else Path(root, id, type.filename)
}