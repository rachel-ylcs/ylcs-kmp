package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Flaky
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.data.rachel.game.info.SAResult
import love.yinlin.extension.Int
import love.yinlin.extension.timeString
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot
import love.yinlin.ui.screen.community.BoxText

@Composable
fun ColumnScope.SearchAllCardInfo(game: GamePublicDetails) {
    val info = remember(game) {
        try { game.info.to<SAInfo>() } catch (_: Throwable) { null }
    }
    if (info != null) {
        RachelText(
            text = remember(info) { "准确率: ${(info.threshold * 100).toInt()}%" },
            icon = Icons.Outlined.Flaky
        )
        RachelText(
            text = remember(info) { "时间限制: ${(info.timeLimit * 1000).toLong().timeString}" },
            icon = Icons.Outlined.Alarm
        )
    }
}

@Stable
class SearchAllCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = SAConfig

    private var threshold by mutableFloatStateOf(0f)
    private var timeLimit by mutableFloatStateOf(0.05f)
    private val inputState = TextInputState()
    private val items = mutableStateSetOf<String>()

    override val canSubmit: Boolean by derivedStateOf { items.size in SAConfig.minCount ..SAConfig.maxCount }

    override val submitInfo: JsonElement get() = SAInfo(
        threshold = threshold.cast(SAConfig.minThreshold, SAConfig.maxThreshold),
        timeLimit = timeLimit.cast(SAConfig.minTimeLimit, SAConfig.maxTimeLimit)
    ).toJson()

    override val submitQuestion: JsonElement get() = JsonPrimitive(items.size)

    override val submitAnswer: JsonElement get() = items.toSet().toJson()

    private fun addOption() {
        val text = inputState.text
        if (text.length in SAConfig.minLength ..SAConfig.maxLength) {
            items.add(text)
            inputState.text = ""
        }
    }

    @Composable
    override fun ColumnScope.Content() {
        GameSlider(
            title = "准确率",
            progress = threshold,
            minValue = SAConfig.minThreshold,
            maxValue = SAConfig.maxThreshold,
            onProgressChange = { threshold = it },
            modifier = Modifier.fillMaxWidth()
        )
        GameSlider(
            title = "时间限制(秒)",
            progress = timeLimit,
            minValue = SAConfig.minTimeLimit,
            maxValue = SAConfig.maxTimeLimit,
            onProgressChange = { timeLimit = it },
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = inputState,
            hint = "备选答案 ${items.size}",
            maxLength = SAConfig.maxLength,
            clearButton = false,
            onImeClick = { addOption() },
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(modifier = Modifier.fillMaxWidth().aspectRatio(1f).verticalScroll(rememberScrollState())) {
            for (item in items) {
                BoxText(
                    text = item,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { items.remove(item) }
                )
            }
        }
    }
}

@Stable
class SearchAllPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val info: SAInfo, val count: Int)

    override val config = SAConfig

    private var preflight: Preflight? by mutableStateOf(null)
    private var result: SAResult? by mutableStateOf(null)

    private val inputState = TextInputState()
    private val items = mutableStateSetOf<String>()

    override val canSubmit: Boolean by derivedStateOf { items.size in SAConfig.minCount ..(preflight?.count ?: SAConfig.maxCount) }

    override val submitAnswer: JsonElement get() = items.toSet().toJson()

    private fun addOption() {
        val text = inputState.text
        if (text.length in SAConfig.minLength ..SAConfig.maxLength) {
            items.add(text)
            inputState.text = ""
        }
    }

    override fun init(preflightResult: PreflightResult) {
        preflight = try {
            inputState.text = ""
            items.clear()
            Preflight(
                info = preflightResult.info.to<SAInfo>(),
                count = preflightResult.question.Int,
            )
        } catch (_: Throwable) { null }
    }

    override fun settle(gameResult: GameResult) {
        result = try {
            gameResult.info.to()
        } catch (_: Throwable) { null }
    }

    @Composable
    override fun ColumnScope.Content() {
        preflight?.let { (_, question) ->
            TextInput(
                state = inputState,
                hint = "答案 ${items.size} / $question",
                maxLength = SAConfig.maxLength,
                clearButton = false,
                onImeClick = { addOption() },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                for (item in items) {
                    BoxText(
                        text = item,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { items.remove(item) }
                    )
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {
        result?.let { (correctCount, totalCount, duration) ->
            Text(
                text = "结算: $correctCount / $totalCount",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "用时: ${(duration.toLong() * 1000).timeString}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}