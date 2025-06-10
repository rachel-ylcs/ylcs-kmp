package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.ui.screen.SubScreenSlot


@Stable
interface CreateGameState {
    val config: GameConfig

    val canSubmit: Boolean

    val submitInfo: JsonElement
    val submitQuestion: JsonElement
    val submitAnswer: JsonElement

    @Composable
    fun ColumnScope.Content()

    @Composable
    fun Floating() {}
}

@Stable
interface PlayGameState

@Stable
interface GameRankingState

@Stable
object GameStateManager {
    fun createGame(type: Game, slot: SubScreenSlot): CreateGameState = when (type) {
        Game.AnswerQuestion -> AnswerQuestionCreateGameState(slot)
        Game.BlockText -> BlockTextCreateGameState(slot)
        Game.FlowersOrder -> FlowersOrderCreateGameState(slot)
        Game.SearchAll -> SearchAllCreateGameState(slot)
    }
}