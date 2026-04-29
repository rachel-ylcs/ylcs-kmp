package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.common.BlockResult

@Stable
data class RhymeSkillResult(
    val result: BlockResult,
    val active: Boolean = false,
    val addCombo: Boolean = result != BlockResult.MISS && result != BlockResult.BAD,
    val dirty: Boolean = active
)