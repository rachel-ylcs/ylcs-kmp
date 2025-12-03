package love.yinlin.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import love.yinlin.compose.*
import love.yinlin.compose.screen.ScreenSlot
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.data.rachel.game.info.SAResult
import love.yinlin.extension.Int
import love.yinlin.extension.catchingNull
import love.yinlin.extension.timeString
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.extension.json
import love.yinlin.screen.community.BoxText

@Composable
fun ColumnScope.SearchAllCardInfo(game: GamePublicDetailsWithName) {
    val info = remember(game) {
        catchingNull { game.info.to<SAInfo>() }
    }
    if (info != null) {
        NormalText(
            text = remember(info) { "准确率: ${(info.threshold * 100).toInt()}%" },
            icon = Icons.Outlined.Flaky
        )
        NormalText(
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
class SearchAllCreateGameState(val slot: ScreenSlot) : CreateGameState {
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

    override val submitQuestion: JsonElement get() = items.size.json

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
class SearchAllPlayGameState(val slot: ScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val info: SAInfo, val count: Int)

    override val config = SAConfig

    private var preflight: Preflight? by mutableRefStateOf(null)
    private var result: SAResult? by mutableRefStateOf(null)

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
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.verticalSpace)
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