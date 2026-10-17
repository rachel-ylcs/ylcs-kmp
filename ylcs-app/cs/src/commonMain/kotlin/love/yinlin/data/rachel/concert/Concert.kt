package love.yinlin.data.rachel.concert

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Concert(
    val cid: Int, // [演出 ID]
    val date: String, // [演出日期，yyyy-MM-dd]
    val location: String? = null, // [地点]
    val title: String, // [标题]
    val poster: String? = null, // [海报]
    val songs: List<Int> = [], // [concert_song.sid 列表，保留歌单顺序]
)
