package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.ui.component.input.BeautifulSlider
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
interface PlayGameState {
    val config: GameConfig

    val canSubmit: Boolean

    val submitAnswer: JsonElement

    fun reset()

    @Composable
    fun ColumnScope.Content(preflightResult: PreflightResult)

    @Composable
    fun ColumnScope.Settlement(gameResult: GameResult)
}

@Stable
interface GameRankingState

fun createGameState(type: Game, slot: SubScreenSlot): CreateGameState = when (type) {
    Game.AnswerQuestion -> AnswerQuestionCreateGameState(slot)
    Game.BlockText -> BlockTextCreateGameState(slot)
    Game.FlowersOrder -> FlowersOrderCreateGameState(slot)
    Game.SearchAll -> SearchAllCreateGameState(slot)
}

fun playGameState(type: Game, slot: SubScreenSlot): PlayGameState = when (type) {
    Game.AnswerQuestion -> AnswerQuestionPlayGameState(slot)
    Game.BlockText -> BlockTextPlayGameState(slot)
    Game.FlowersOrder -> FlowersOrderPlayGameState(slot)
    Game.SearchAll -> SearchAllPlayGameState(slot)
}

@Composable
fun ColumnScope.GameCardInfo(game: GamePublicDetails) = when (game.type) {
    Game.AnswerQuestion -> AnswerQuestionCardInfo(game)
    Game.BlockText -> BlockTextCardInfo(game)
    Game.FlowersOrder -> FlowersOrderCardInfo(game)
    Game.SearchAll -> SearchAllCardInfo(game)
}

internal fun Float.cast(minValue: Int, maxValue: Int): Int = (this * (maxValue - minValue) + minValue).toInt()
internal fun Float.cast(minValue: Float, maxValue: Float): Float = this * (maxValue - minValue) + minValue

@Composable
fun <T : Number> GameSlider(
    title: String,
    progress: Float,
    minValue: T,
    maxValue: T,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .border(width = ThemeValue.Border.Medium, color = MaterialTheme.colorScheme.primary)
                .padding(ThemeValue.Padding.EqualExtraValue),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BeautifulSlider(
                    value = progress,
                    onValueChangeFinished = onProgressChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.EqualSpace),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                ) {
                    Text(text = minValue.toString())
                    Text(
                        text = remember(progress, minValue, maxValue) {
                            when (minValue) {
                                is Int if maxValue is Int -> progress.cast(minValue, maxValue).toString()
                                is Float if maxValue is Float -> progress.cast(minValue, maxValue).toString()
                                else -> "error"
                            }
                        },
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = maxValue.toString())
                }
            }
        }
    }
}