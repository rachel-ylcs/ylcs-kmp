package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Flaky
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.*
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.data.rachel.game.info.SAResult
import love.yinlin.extension.*
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot
import love.yinlin.ui.screen.community.BoxText

@Composable
fun ColumnScope.SearchAllCardInfo(game: GamePublicDetailsWithName) {
    val info = remember(game) {
        catchingNull { game.info.to<SAInfo>() }
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

@Composable
fun ColumnScope.SearchAllCardQuestionAnswer(game: GameDetailsWithName) {
    val answer = remember(game) {
        catchingNull { game.answer.to<List<String>>() }
    }
    if (answer != null) {
        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        FlowRow(modifier = Modifier.fillMaxWidth().aspectRatio(1f).verticalScroll(rememberScrollState())) {
            for (item in answer) {
                BoxText(
                    text = item,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SearchAllRecordResult(result: SAResult) {
    val (correctCount, totalCount, duration) = result
    Text(
        text = "正确率: $correctCount / $totalCount",
        style = MaterialTheme.typography.titleLarge,
        textAlign = Center,
        maxLines = 1,
        overflow = Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "用时: ${(duration.toLong() * 1000).timeString}",
        style = MaterialTheme.typography.titleLarge,
        textAlign = Center,
        maxLines = 1,
        overflow = Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ColumnScope.SearchAllRecordCard(answer: JsonElement, info: JsonElement) {
    val data = remember(answer, info) {
        catchingNull { answer.to<List<String>>() to info.to<SAResult>() }
    }

    data?.let { (actualAnswer, actualResult) ->
        SearchAllRecordResult(actualResult)

        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        FlowRow(modifier = Modifier.fillMaxWidth().aspectRatio(1f).verticalScroll(rememberScrollState())) {
            for (item in actualAnswer) {
                BoxText(
                    text = item,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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
    private var time by mutableLongStateOf(0L)

    override val canSubmit: Boolean by derivedStateOf { items.size in SAConfig.minCount ..(preflight?.count ?: SAConfig.maxCount) }

    override val submitAnswer: JsonElement get() = items.toSet().toJson()

    private fun addOption() {
        val text = inputState.text
        if (text.length in SAConfig.minLength ..SAConfig.maxLength) {
            items.add(text)
            inputState.text = ""
        }
    }

    override fun init(scope: CoroutineScope, preflightResult: PreflightResult) {
        preflight = catchingNull {
            inputState.text = ""
            items.clear()
            val info = preflightResult.info.to<SAInfo>()
            require(info.timeLimit in SAConfig.minTimeLimit .. SAConfig.maxTimeLimit)
            time = info.timeLimit * 1000L
            Preflight(
                info = info,
                count = preflightResult.question.Int,
            )
        }
        if (preflight != null) scope.launch {
            while (true) {
                if (time > 1000L) time -= 1000L
                else if (time > 0L) time = 0L
                else break
                delay(1000L)
            }
        }
    }

    override fun settle(gameResult: GameResult) {
        result = catchingNull { gameResult.info.to() }
    }

    @Composable
    override fun ColumnScope.Content() {
        preflight?.let { (_, question) ->
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Text(
                text = remember(time) { time.timeString },
                style = MaterialTheme.typography.labelLarge,
                textAlign = Center,
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.VerticalSpace)
            )
            TextInput(
                state = inputState,
                hint = "答案 ${items.size} / $question",
                maxLength = SAConfig.maxLength,
                clearButton = false,
                onImeClick = { addOption() },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
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
        result?.let { SearchAllRecordResult(it) }
    }
}