package love.yinlin.data.rachel.photo

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.cs.ServerRes

@Stable
@Serializable
data class PhotoAlbum(
    val aid: Int, // ID
    val name: String, // 名称
    val title: String, // 标题
    val ts: String, // 时间
    val location: String?, // 地点
    val author: String?, // 作者
    val keyword: List<String>, // 关键字
    val picNum: Int, // 图片数
) {
    fun picPath(index: Int) = ServerRes.Photo.pic(name, index, false)
    fun thumbPath(index: Int) = ServerRes.Photo.pic(name, index, true)

    companion object {
        const val MAX_NUM = 100
    }
}

typealias PhotoAlbumList = List<PhotoAlbum>