package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.api.ServerRes
import kotlin.getValue

@Stable
@Serializable
data class ActivityPhoto(
    val cover: String? = null, // [活动封面]
    val seat: String? = null, // [座位图]
    val posters: List<String> = emptyList(), // [海报]
) {
    val coverPath by lazy { cover?.let { ServerRes.Activity.activity(it) } }

    val seatPath by lazy { seat?.let { ServerRes.Activity.activity(it) } }

    fun posterPath(key: String) = ServerRes.Activity.activity(key)
}