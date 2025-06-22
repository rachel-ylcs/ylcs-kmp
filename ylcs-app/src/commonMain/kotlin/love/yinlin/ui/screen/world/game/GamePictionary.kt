package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.PConfig
import love.yinlin.data.rachel.game.info.PaintPath
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.container.PaintCanvas
import love.yinlin.ui.component.container.PaintCanvasState
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot

@Composable
fun ColumnScope.PictionaryCardInfo(game: GamePublicDetailsWithName) {}

@Composable
fun ColumnScope.PictionaryQuestionAnswer(game: GameDetailsWithName) {
    val data = remember(game) {
        catchingNull { game.question.to<Pair<List<PaintPath>, Int>>() to game.answer.String }
    }
    data?.let { (question, answer) ->
        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        RachelText(
            text = answer,
            icon = Icons.Outlined.Lightbulb,
            color = MaterialTheme.colorScheme.primary
        )
        PaintCanvas(
            state = remember(question) { PaintCanvasState(question.first) },
            enabled = false,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ColumnScope.PictionaryRecordCard(answer: JsonElement, info: JsonElement) {
    val data = remember(answer, info) {
        catchingNull { answer.String }
    }
    if (data != null) {
        Text(
            text = "我的答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        RachelText(
            text = data,
            icon = Icons.Outlined.Lightbulb
        )
    }
}

@Stable
class PictionaryCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: PConfig = PConfig

    private val paintState = PaintCanvasState()
    private val inputState = TextInputState()

    override val canSubmit: Boolean by derivedStateOf {
        paintState.paths.isNotEmpty() && inputState.text.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength
    }

    override val submitInfo: JsonElement = Unit.toJson()

    override val submitQuestion: JsonElement get() = (paintState.paths.toList() to inputState.text.length).toJson()

    override val submitAnswer: JsonElement get() = inputState.text.toJson()

    @Composable
    override fun ColumnScope.Content() {
        TextInput(
            state = inputState,
            hint = "答案(长度${PConfig.minAnswerLength} - ${PConfig.maxAnswerLength})",
            maxLength = PConfig.maxAnswerLength,
            modifier = Modifier.fillMaxWidth()
        )
        PaintCanvas(
            state = paintState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Stable
class PictionaryPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val paths: List<PaintPath>, val count: Int)

    override val config: PConfig = PConfig

    private var preflight: Preflight? by mutableStateOf(null)
    private var result: Unit? by mutableStateOf(null)

    private val inputState = TextInputState()

    override val canSubmit: Boolean by derivedStateOf { inputState.text.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength }

    override val submitAnswer: JsonElement get() = inputState.text.toJson()

    override fun init(scope: CoroutineScope, preflightResult: PreflightResult) {
        preflight = catchingNull {
            val (paths, length) = preflightResult.question.to<Pair<List<PaintPath>, Int>>()
            require(length in PConfig.minAnswerLength ..PConfig.maxAnswerLength)
            require(paths.isNotEmpty())
            Preflight(paths = paths, count = length)
        }
    }

    override fun settle(gameResult: GameResult) {
        result = catchingNull { gameResult.info.to() }
    }

    @Composable
    override fun ColumnScope.Content() {
        preflight?.let { (paths, count) ->
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            TextInput(
                state = inputState,
                hint = "答案(字数:$count)",
                maxLength = PConfig.maxAnswerLength,
                clearButton = false,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            PaintCanvas(
                state = remember(paths) { PaintCanvasState(paths) },
                enabled = false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {}
}