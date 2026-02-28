package love.yinlin.common.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.common.PlayGameState
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.widget.PaintCanvas
import love.yinlin.compose.ui.widget.PaintCanvasState
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.PConfig
import love.yinlin.data.rachel.game.info.PictionaryQuestion
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson

@Stable
object PictionaryMapper : GameMapper(), GameAnswerInfo, GameRecordInfo {
    @Composable
    override fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName) {
        val data = remember(gameDetails) {
            catchingNull { gameDetails.question.to<PictionaryQuestion>() to gameDetails.answer.String }
        }

        data?.let { (question, answer) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "答案", style = Theme.typography.v6.bold, modifier = Modifier.idText())
            }
            Space()
            SimpleEllipsisText(text = answer, color = Theme.color.primary)
            PaintCanvas(
                state = remember(question) { PaintCanvasState(question.paths) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(fraction = 0.7f).align(Alignment.CenterHorizontally)
            )
        }
    }

    @Composable
    override fun ColumnScope.GameRecordInfoContent(data: GameRecordInfo.Data) {
        val answer = remember(data) {
            catchingNull { data.answer.String }
        }

        if (answer != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "我的答案", modifier = Modifier.idText())
            }
            SimpleEllipsisText(text = answer, color = Theme.color.primary)
        }
    }

    @Stable
    class PCreateGameState(parent: BasicScreen) : CreateGameState {
        private val paintState = PaintCanvasState()
        private val inputState = InputState(maxLength = PConfig.maxAnswerLength)

        override val config = PConfig

        override val canSubmit: Boolean by derivedStateOf {
            paintState.paths.isNotEmpty() && inputState.text.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength
        }

        override val submitInfo: JsonElement = Unit.toJson()

        override val submitQuestion: JsonElement get() = PictionaryQuestion(paintState.paths.toList(), inputState.text.length).toJson()

        override val submitAnswer: JsonElement get() = inputState.text.toJson()

        @Composable
        override fun ColumnScope.ConfigContent() { }

        @Composable
        override fun ColumnScope.Content() {
            Input(
                state = inputState,
                hint = "答案(长度${PConfig.minAnswerLength}~${PConfig.maxAnswerLength})",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                trailing = InputDecoration.LengthViewer
            )
            PaintCanvas(state = paintState, modifier = Modifier.fillMaxWidth())
        }

        @Composable
        override fun Floating() { }
    }

    override val gameCreator: (BasicScreen) -> CreateGameState = ::PCreateGameState

    @Stable
    class PPlayGameState(parent: BasicScreen) : PlayGameState {
        @Stable
        private data class Preflight(val question: PictionaryQuestion)

        override val config: PConfig = PConfig

        private var preflight: Preflight? by mutableRefStateOf(null)
        private var result: Unit? by mutableRefStateOf(null)

        private val inputState = InputState(maxLength = PConfig.maxAnswerLength)

        override val canSubmit: Boolean by derivedStateOf { inputState.text.length in PConfig.minAnswerLength .. PConfig.maxAnswerLength }

        override val submitAnswer: JsonElement get() = inputState.text.toJson()

        override fun init(preflightResult: PreflightResult) {
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

                Input(
                    state = inputState,
                    hint = "答案(字数:${question.count})",
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    trailing = InputDecoration.LengthViewer
                )

                PaintCanvas(
                    state = remember { PaintCanvasState(question.paths) },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        @Composable
        override fun ColumnScope.Settlement() { }

        @Composable
        override fun Floating() { }
    }

    override val gamePlayer: (BasicScreen) -> PlayGameState = ::PPlayGameState
}