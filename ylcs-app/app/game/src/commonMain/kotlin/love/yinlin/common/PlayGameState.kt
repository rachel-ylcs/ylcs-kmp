package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult

@Stable
interface PlayGameState {
    val config: GameConfig

    val canSubmit: Boolean

    val submitAnswer: JsonElement

    fun init(preflightResult: PreflightResult)

    fun settle(gameResult: GameResult)

    @Composable
    fun ColumnScope.Content()

    @Composable
    fun ColumnScope.Settlement()

    @Composable
    fun Floating()
}