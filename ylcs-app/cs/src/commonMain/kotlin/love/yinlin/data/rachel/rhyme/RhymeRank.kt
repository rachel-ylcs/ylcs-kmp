package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class RhymeRank(
    val rid: Long,
    val uid: Int,
    val name: String,
    val result: RhymeUploadResult
)