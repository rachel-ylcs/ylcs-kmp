package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.data.rachel.game.GamePublicDetailsWithName

@Stable
fun interface GameItemExtraInfo {
    @Composable
    fun ColumnScope.GameItemExtraInfoContent(gameDetails: GamePublicDetailsWithName)
}