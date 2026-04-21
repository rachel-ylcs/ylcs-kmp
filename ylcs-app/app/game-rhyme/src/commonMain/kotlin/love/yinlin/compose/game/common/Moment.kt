package love.yinlin.compose.game.common

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.traits.Visible

@Stable
data class Moment(val start: Long, val builder: () -> Visible)