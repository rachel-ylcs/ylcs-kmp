package love.yinlin.data.rachel.concert

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ConcertSaveRequest(
    val cid: Int?,
    val date: String,
    val location: String?,
    val title: String,
    val poster: String?,
    val songIds: List<Int?>,
    val newSongNames: List<String>
)
