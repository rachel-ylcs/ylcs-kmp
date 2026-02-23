package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.data.rachel.game.GameDetailsWithName

@Stable
fun interface GameAnswerInfo {
    @Composable
    fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName)
}