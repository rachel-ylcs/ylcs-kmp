package love.yinlin.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameConfig

@Stable
interface CreateGameState {
    val config: GameConfig

    val canSubmit: Boolean

    val submitInfo: JsonElement
    val submitQuestion: JsonElement
    val submitAnswer: JsonElement

    @Composable
    fun ColumnScope.ConfigContent()

    @Composable
    fun ColumnScope.Content()

    @Composable
    fun Floating()
}