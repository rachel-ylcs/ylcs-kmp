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
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.PConfig
import love.yinlin.data.rachel.game.info.PictionaryQuestion
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.container.PaintCanvas
import love.yinlin.ui.component.container.PaintCanvasState
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.ui.screen.SubScreenSlot

@Composable
fun ColumnScope.PictionaryCardInfo(game: GamePublicDetailsWithName) {}

@Composable
fun ColumnScope.PictionaryQuestionAnswer(game: GameDetailsWithName) {
    val data = remember(game) {
        catchingNull { game.question.to<PictionaryQuestion>() to game.answer.String }
    }
    data?.let { (question, answer) ->
        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        NormalText(
            text = answer,
            icon = Icons.Outlined.Lightbulb,
            color = MaterialTheme.colorScheme.primary
        )
        PaintCanvas(
            state = remember(question) { PaintCanvasState(question.paths) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
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
        NormalText(
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

    override val submitQuestion: JsonElement get() = PictionaryQuestion(paintState.paths.toList(), inputState.text.length).toJson()

    override val submitAnswer: JsonElement get() = inputState.text.toJson()

    @Composable
    override fun ColumnScope.Content() {
        TextInput(
            state = inputState,
            hint = "答案(长度${PConfig.minAnswerLength}~${PConfig.maxAnswerLength})",
            maxLength = PConfig.maxAnswerLength,
            modifier = Modifier.fillMaxWidth()
        )
        PaintCanvas(
            state = paintState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Stable
class PictionaryPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val question: PictionaryQuestion)

    override val config: PConfig = PConfig

    private var preflight: Preflight? by mutableRefStateOf(null)
    private var result: Unit? by mutableRefStateOf(null)

    private val inputState = TextInputState()

    override val canSubmit: Boolean by derivedStateOf { inputState.text.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength }

    override val submitAnswer: JsonElement get() = inputState.text.toJson()

    override fun init(scope: CoroutineScope, preflightResult: PreflightResult) {
        preflight = catchingNull {
            val question = preflightResult.question.to<PictionaryQuestion>()
            require(question.count in PConfig.minAnswerLength ..PConfig.maxAnswerLength)
            require(question.paths.isNotEmpty())
            Preflight(question = question)
        }
    }

    override fun settle(gameResult: GameResult) {
        result = catchingNull { gameResult.info.to() }
    }

    @Composable
    override fun ColumnScope.Content() {
        preflight?.let { (question) ->
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            TextInput(
                state = inputState,
                hint = "答案(字数:${question.count})",
                maxLength = PConfig.maxAnswerLength,
                clearButton = false,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            PaintCanvas(
                state = remember(question) { PaintCanvasState(question.paths) },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {}
}