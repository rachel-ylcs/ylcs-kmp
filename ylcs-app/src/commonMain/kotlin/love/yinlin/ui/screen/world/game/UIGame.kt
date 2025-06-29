package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import love.yinlin.Local
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.CylinderSlider
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.screen.SubScreenSlot
import love.yinlin.ui.screen.community.BoxText

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

    fun init(scope: CoroutineScope, preflightResult: PreflightResult)

    fun settle(gameResult: GameResult)

    @Composable
    fun ColumnScope.Content()

    @Composable
    fun ColumnScope.Settlement()

    @Composable
    fun Floating() {}
}

fun createGameState(type: Game, slot: SubScreenSlot): CreateGameState = when (type) {
    Game.AnswerQuestion -> AnswerQuestionCreateGameState(slot)
    Game.BlockText -> BlockTextCreateGameState(slot)
    Game.FlowersOrder -> FlowersOrderCreateGameState(slot)
    Game.SearchAll -> SearchAllCreateGameState(slot)
    Game.Pictionary -> PictionaryCreateGameState(slot)
    Game.GuessLyrics, Game.Rhyme -> error("Unknown type $type")
}

fun playGameState(type: Game, slot: SubScreenSlot): PlayGameState = when (type) {
    Game.AnswerQuestion -> AnswerQuestionPlayGameState(slot)
    Game.BlockText -> BlockTextPlayGameState(slot)
    Game.FlowersOrder -> FlowersOrderPlayGameState(slot)
    Game.SearchAll -> SearchAllPlayGameState(slot)
    Game.Pictionary -> PictionaryPlayGameState(slot)
    Game.GuessLyrics, Game.Rhyme -> error("Unknown type $type")
}

@Composable
fun ColumnScope.GameCardInfo(game: GamePublicDetailsWithName) = when (game.type) {
    Game.AnswerQuestion -> AnswerQuestionCardInfo(game)
    Game.BlockText -> BlockTextCardInfo(game)
    Game.FlowersOrder -> FlowersOrderCardInfo(game)
    Game.SearchAll -> SearchAllCardInfo(game)
    Game.Pictionary -> PictionaryCardInfo(game)
    Game.GuessLyrics, Game.Rhyme -> error("Unknown type ${game.type}")
}

@Composable
fun ColumnScope.GameCardQuestionAnswer(game: GameDetailsWithName) = when (game.type) {
    Game.AnswerQuestion -> AnswerQuestionCardQuestionAnswer(game)
    Game.BlockText -> BlockTextCardQuestionAnswer(game)
    Game.FlowersOrder -> FlowersOrderCardQuestionAnswer(game)
    Game.SearchAll -> SearchAllCardQuestionAnswer(game)
    Game.Pictionary -> PictionaryQuestionAnswer(game)
    Game.GuessLyrics, Game.Rhyme -> error("Unknown type ${game.type}")
}

@Composable
fun ColumnScope.GameRecordCard(type: Game, answer: JsonElement, info: JsonElement) = when (type) {
    Game.AnswerQuestion -> AnswerQuestionRecordCard(answer, info)
    Game.BlockText -> BlockTextRecordCard(answer, info)
    Game.FlowersOrder -> FlowersOrderRecordCard(answer, info)
    Game.SearchAll -> SearchAllRecordCard(answer, info)
    Game.Pictionary -> PictionaryRecordCard(answer, info)
    Game.GuessLyrics, Game.Rhyme -> error("Unknown type $type")
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
    CylinderSlider(
        value = progress,
        onValueChanged = onProgressChange,
        modifier = modifier
    ) { percent ->
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = remember(percent, minValue, maxValue) {
                    "当前: ${when (minValue) {
                        is Int if maxValue is Int -> percent.cast(minValue, maxValue)
                        is Float if maxValue is Float -> percent.cast(minValue, maxValue)
                        else -> "N/A"
                    }}"
                },
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = remember(percent, minValue, maxValue) { "范围: $minValue ~ $maxValue" },
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GameItem(
    game: GamePublicDetailsWithName,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = ThemeValue.Shadow.Surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(ThemeValue.Padding.ExtraValue),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WebImage(
                    uri = remember { game.type.yPath },
                    key = Local.VERSION,
                    contentScale = ContentScale.Crop,
                    circle = true,
                    modifier = Modifier.size(ThemeValue.Size.MediumImage)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                ) {
                    RachelText(text = game.name, icon = Icons.Outlined.AccountCircle)
                    RachelText(text = game.ts, icon = Icons.Outlined.Timer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RachelText(
                            text = game.reward.toString(),
                            icon = Icons.Outlined.Diamond,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        RachelText(
                            text = game.num.toString(),
                            icon = Icons.Outlined.FormatListNumbered,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        RachelText(
                            text = game.cost.toString(),
                            icon = Icons.Outlined.Paid,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            Text(
                text = game.title,
                modifier = Modifier.fillMaxWidth()
            )
            GameCardInfo(game)
            if (game.winner.isNotEmpty()) {
                RachelText(text = "赢家", icon = Icons.Outlined.MilitaryTech)
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    game.winner.fastForEach { winner ->
                        BoxText(text = winner, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            content()
        }
    }
}