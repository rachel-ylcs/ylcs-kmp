package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes2
import kotlin.getValue

@Stable
@Serializable
data class ActivityPhoto(
    val cover: String? = null, // [活动封面]
    val seat: String? = null, // [座位图]
    val posters: List<String> = emptyList(), // [海报]
) {
    val coverPath: String? by lazy { cover?.let { "${Local.API_BASE_URL}/${ServerRes2.Activity.activity(it)}" } }

    val seatPath: String? by lazy { seat?.let { "${Local.API_BASE_URL}/${ServerRes2.Activity.activity(it)}" } }

    fun posterPath(key: String): String = "${Local.API_BASE_URL}/${ServerRes2.Activity.activity(key)}"
}