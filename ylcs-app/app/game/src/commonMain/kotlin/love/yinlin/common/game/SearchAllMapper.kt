package love.yinlin.common.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameItemExtraInfo
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.common.PlayGameState
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.common.ArgsSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.data.rachel.game.info.SAResult
import love.yinlin.extension.Int
import love.yinlin.extension.catchingNull
import love.yinlin.extension.json
import love.yinlin.extension.timeString
import love.yinlin.extension.to
import love.yinlin.extension.toJson

@Stable
object SearchAllMapper : GameMapper(), GameItemExtraInfo, GameAnswerInfo, GameRecordInfo {
    @Composable
    override fun ColumnScope.GameItemExtraInfoContent(gameDetails: GamePublicDetailsWithName) {
        val info = remember(gameDetails) { catchingNull { gameDetails.info.to<SAInfo>() } }
        if (info != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "准确率: ${(info.threshold * 100).toInt()}%", style = Theme.typography.v8, modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Alarm, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "时间限制: ${(info.timeLimit * 1000).toLong().timeString}", style = Theme.typography.v8, modifier = Modifier.idText())
            }
        }
    }

    @Composable
    override fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName) {
        val answer = remember(gameDetails) { catchingNull { gameDetails.answer.to<List<String>>() } }

        if (answer != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "答案", style = Theme.typography.v6.bold, modifier = Modifier.idText())
            }
            Space()
            TagView(
                size = answer.size,
                titleProvider = { answer[it] },
                readonly = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }
    }

    @Composable
    override fun ColumnScope.GameRecordInfoContent(data: GameRecordInfo.Data) {
        val pairData = remember(data) {
            val (answer, info) = data
            catchingNull { answer.to<List<String>>() to info.to<SAResult>() }
        }

        pairData?.let { (actualAnswer, actualResult) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "正确率: ${actualResult.correctCount} / ${actualResult.totalCount}", modifier = Modifier.idText())
            }
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "用时: ${(actualResult.duration.toLong() * 1000).timeString}", modifier = Modifier.idText())
            }

            TagView(
                size = actualAnswer.size,
                titleProvider = { actualAnswer[it] },
                readonly = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }
    }

    @Stable
    class SACreateGameState(parent: BasicScreen) : CreateGameState {
        private var threshold by mutableRefStateOf(SliderArgs(SAConfig.minThreshold, SAConfig.minThreshold, SAConfig.maxThreshold))
        private var timeLimit by mutableRefStateOf(SliderArgs(SAConfig.minTimeLimit, SAConfig.minTimeLimit, SAConfig.maxTimeLimit))
        private val inputState = InputState(maxLength = SAConfig.maxLength)
        private val items = mutableStateSetOf<String>()
        private val tags by derivedStateOf { items.toList() }

        override val config = SAConfig

        override val canSubmit: Boolean by derivedStateOf { items.size in SAConfig.minCount ..SAConfig.maxCount }

        override val submitInfo: JsonElement get() = SAInfo(threshold = threshold.value, timeLimit = timeLimit.value).toJson()

        override val submitQuestion: JsonElement get() = items.size.json

        override val submitAnswer: JsonElement get() = items.toSet().toJson()

        @Composable
        override fun ColumnScope.ConfigContent() {
            ArgsSlider(
                title = "准确率",
                args = threshold,
                onValueChange = { threshold = threshold.copy(tmpValue = it) },
                modifier = Modifier.fillMaxWidth()
            )
            ArgsSlider(
                title = "时间限制(秒)",
                args = timeLimit,
                onValueChange = { timeLimit = timeLimit.copy(tmpValue = it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun ColumnScope.Content() {
            Input(
                state = inputState,
                hint = "备选答案 ${items.size}",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                trailing = InputDecoration.LengthViewer,
                onImeClick = {
                    val text = inputState.text
                    if (text.length in SAConfig.minLength ..SAConfig.maxLength) {
                        items.add(text)
                        inputState.text = ""
                    }
                }
            )

            TagView(
                size = tags.size,
                titleProvider = { tags[it] },
                onDelete = { items.remove(tags[it]) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun Floating() { }
    }

    override val gameCreator: (BasicScreen) -> CreateGameState = ::SACreateGameState

    @Stable
    class SAPlayGameState(val parent: BasicScreen) : PlayGameState {
        @Stable
        private data class Preflight(val info: SAInfo, val count: Int)

        override val config = SAConfig

        private var preflight: Preflight? by mutableRefStateOf(null)
        private var result: SAResult? by mutableRefStateOf(null)

        private val inputState = InputState(maxLength = SAConfig.maxLength)
        private val items = mutableStateSetOf<String>()
        private val tags by derivedStateOf { items.toList() }
        private var time by mutableLongStateOf(0L)

        override val canSubmit: Boolean by derivedStateOf { items.size in SAConfig.minCount ..(preflight?.count ?: SAConfig.maxCount) }

        override val submitAnswer: JsonElement get() = items.toSet().toJson()

        override fun init(preflightResult: PreflightResult) {
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
            if (preflight != null) {
                parent.launch {
                    while (true) {
                        if (time > 1000L) time -= 1000L
                        else if (time > 0L) time = 0L
                        else break
                        delay(1000L)
                    }
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

                TextIconAdapter(modifier = Modifier.align(Alignment.CenterHorizontally).padding(Theme.padding.value)) { idIcon, idText ->
                    Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = time.timeString, modifier = Modifier.idText())
                }

                Input(
                    state = inputState,
                    hint = "答案 ${items.size} / $question",
                    onImeClick = {
                        val text = inputState.text
                        if (text.length in SAConfig.minLength ..SAConfig.maxLength) {
                            items.add(text)
                            inputState.text = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )

                TagView(
                    size = tags.size,
                    titleProvider = { tags[it] },
                    onDelete = { items.remove(tags[it]) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        @Composable
        override fun ColumnScope.Settlement() {
            result?.let {
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = "正确率: ${it.correctCount} / ${it.totalCount}", modifier = Modifier.idText())
                }
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = "用时: ${(it.duration.toLong() * 1000).timeString}", modifier = Modifier.idText())
                }
            }
        }

        @Composable
        override fun Floating() { }
    }

    override val gamePlayer: (BasicScreen) -> PlayGameState = ::SAPlayGameState
}