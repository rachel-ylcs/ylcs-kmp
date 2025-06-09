package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class BTResult(val correctCount: Int, val totalCount: Int)