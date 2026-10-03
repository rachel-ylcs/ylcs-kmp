package love.yinlin.compose.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable(with = ItemKeySerializer::class)
actual data class ItemKey actual constructor(internal actual val value: String)