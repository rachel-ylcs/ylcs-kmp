package love.yinlin.common.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameItemExtraInfo
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.common.PlayGameState
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.GameSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.FOConfig
import love.yinlin.data.rachel.game.info.FOInfo
import love.yinlin.data.rachel.game.info.FOType
import love.yinlin.extension.Int
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.json
import love.yinlin.extension.to
import love.yinlin.extension.toJson

@Stable
object FlowersOrderMapper : GameMapper(), GameItemExtraInfo, GameAnswerInfo, GameRecordInfo {
    @Composable
    private fun FlowersOrderText(
        text: String,
        key: Int,
        style: TextStyle = Theme.typography.v7.bold,
        modifier: Modifier = Modifier
    ) {
        val items = remember(key) { FOType.decode(key) }
        Box(modifier = modifier) {
            if (text.length == items.size) {
                val text = remember(text, items) {
                    buildAnnotatedString {
                        text.forEachIndexed { index, ch ->
                            when (items[index]) {
                                FOType.CORRECT -> withStyle(SpanStyle(color = Colors.Green5)) { append(ch) }
                                FOType.INVALID_POS -> withStyle(SpanStyle(color = Colors.Yellow5)) { append(ch) }
                                FOType.INCORRECT -> withStyle(SpanStyle(color = Colors.Red5)) { append(ch) }
                            }
                        }
                    }
                }

                Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    @Composable
    override fun ColumnScope.GameItemExtraInfoContent(gameDetails: GamePublicDetailsWithName) {
        val info = remember(gameDetails) { catchingNull { gameDetails.info.to<FOInfo>() } }
        if (info != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.RestartAlt, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "重试次数: ${info.tryCount}", style = Theme.typography.v8, modifier = Modifier.idText())
            }
        }
    }

    @Composable
    override fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName) {
        val answer = remember(gameDetails) { catchingNull { gameDetails.answer.String } }

        if (answer != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "答案", style = Theme.typography.v6.bold, modifier = Modifier.idText())
            }
            Space()
            SimpleEllipsisText(text = answer, color = Theme.color.primary)
        }
    }

    @Composable
    override fun ColumnScope.GameRecordInfoContent(data: GameRecordInfo.Data) {
        val pairData = remember(data) {
            val (answer, info) = data
            catchingNull { answer.String to info.Int }
        }

        pairData?.let { (actualAnswer, actualResult) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "本次答案", modifier = Modifier.idText())
            }
            FlowersOrderText(actualAnswer, actualResult)
        }
    }

    @Stable
    class FOCreateGameState(parent: BasicScreen) : CreateGameState {
        private var tryCount by mutableRefStateOf(SliderArgs(FOConfig.minTryCount, FOConfig.minTryCount, FOConfig.maxTryCount))
        private val content = InputState(maxLength = FOConfig.maxLength)

        override val config = FOConfig

        override val canSubmit: Boolean by derivedStateOf {
            val text = content.text
            text.length in FOConfig.minLength .. FOConfig.maxLength && text.all { FOType.check(it) }
        }

        override val submitInfo: JsonElement get() = FOInfo(tryCount.value).toJson()

        override val submitQuestion: JsonElement get() = content.text.length.json

        override val submitAnswer: JsonElement get() = content.text.json

        @Composable
        override fun ColumnScope.ConfigContent() {
            GameSlider(
                title = "尝试次数",
                args = tryCount,
                onValueChange = { tryCount = tryCount.copy(tmpValue = it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun ColumnScope.Content() {
            Input(
                state = content,
                hint = "内容(长度${FOConfig.minLength}~${FOConfig.maxLength})",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                trailing = InputDecoration.LengthViewer
            )
        }

        @Composable
        override fun Floating() { }
    }

    override val gameCreator: (BasicScreen) -> CreateGameState = ::FOCreateGameState

    private val CommonDictionary = """
不人月天江春无山花风夜日一
来云水上长见有生城飞处流尽
下百时声中万落君秋相此色心
去闻酒行空欲寒千三黄青衣归
年家朝东阳草看五是客前入马
雪雨金重西海明愁知成为门红
得如多在似谁目烟满树情开头
可南高将出今鸟间道还泪柳望
孤半光转笑楼未我思大古十问
里身五深死歌梦意怜向别关莫
绿百犹照河汉平尘子地四乡两
与难舟独林曲新恨回北须路皆
登清杨更故影当应已少对宫好
""".trimIndent()

    @Stable
    class FOPlayGameState(parent: BasicScreen) : PlayGameState {
        @Stable
        private data class Preflight(
            val length: Int,
            val answer: List<String>,
            val result: List<Int>,
            val oldCharacters: AnnotatedString
        )

        override val config = FOConfig

        private var preflight: Preflight? by mutableRefStateOf(null)
        private var result: Int? by mutableRefStateOf(null)

        private val inputState = InputState()

        override val canSubmit: Boolean by derivedStateOf { inputState.text.length == preflight?.length }

        override val submitAnswer: JsonElement get() = inputState.text.json

        override fun init(preflightResult: PreflightResult) {
            preflight = catchingNull {
                inputState.text = ""
                val answer = preflightResult.answer.to<List<String>>()
                val result = preflightResult.result.to<List<GameResult>>().map { it.info.Int }
                require(answer.size == result.size)
                Preflight(
                    length = preflightResult.question.Int,
                    answer = answer,
                    result = result,
                    oldCharacters = buildAnnotatedString {
                        val correctSet = mutableSetOf<Char>()
                        val incorrectSet = mutableSetOf<Char>()
                        for (i in answer.indices.reversed()) {
                            val v = FOType.decode(result[i])
                            answer[i].forEachIndexed { index, ch ->
                                if (v[index] == FOType.INCORRECT) incorrectSet.add(ch)
                                else correctSet.add(ch)
                            }
                        }
                        val normal = StringBuilder()
                        val correct = StringBuilder()
                        val incorrect = StringBuilder()
                        var charState = 0
                        for (ch in CommonDictionary) {
                            when (ch) {
                                in correctSet -> {
                                    when (charState) {
                                        0 -> {
                                            append(normal)
                                            normal.clear()
                                        }
                                        2 -> {
                                            withStyle(SpanStyle(color = Colors.Red4)) { append(incorrect) }
                                            incorrect.clear()
                                        }
                                    }
                                    charState = 1
                                    correct.append(ch)
                                }
                                in incorrectSet -> {
                                    when (charState) {
                                        0 -> {
                                            append(normal)
                                            normal.clear()
                                        }
                                        1 -> {
                                            withStyle(SpanStyle(color = Colors.Green4)) { append(correct) }
                                            correct.clear()
                                        }
                                    }
                                    charState = 2
                                    incorrect.append(ch)
                                }
                                else -> {
                                    when (charState) {
                                        1 -> {
                                            withStyle(SpanStyle(color = Colors.Green4)) { append(correct) }
                                            correct.clear()
                                        }
                                        2 -> {
                                            withStyle(SpanStyle(color = Colors.Red4)) { append(incorrect) }
                                            incorrect.clear()
                                        }
                                    }
                                    charState = 0
                                    normal.append(ch)
                                }
                            }
                        }
                        when (charState) {
                            0 -> append(normal)
                            1 -> withStyle(SpanStyle(color = Colors.Green4)) { append(correct) }
                            2 -> withStyle(SpanStyle(color = Colors.Red4)) { append(incorrect) }
                        }
                    }
                )
            }
        }

        override fun settle(gameResult: GameResult) {
            result = catchingNull { gameResult.info.Int }
        }

        @Composable
        override fun ColumnScope.Content() {
            preflight?.let { (question, answer, result, oldCharacters) ->
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                SimpleEllipsisText(
                    text = "寻花令长度: $question",
                    style = Theme.typography.v6.bold,
                    color = Theme.color.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Input(
                    state = inputState,
                    hint = "答案",
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )

                Column(
                    modifier = Modifier.fillMaxWidth().dashBorder(Theme.border.v7, Theme.color.primary).padding(Theme.padding.value9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                ) {
                    SimpleEllipsisText(
                        text = "历史记录",
                        style = Theme.typography.v6,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (answer.size == result.size) {
                        repeat(answer.size) { index ->
                            FlowersOrderText(text = answer[index], key = result[index])
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().dashBorder(Theme.border.v7, Theme.color.primary).padding(Theme.padding.value9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                ) {
                    SimpleEllipsisText(
                        text = "常用字表",
                        style = Theme.typography.v6,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = oldCharacters,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        @Composable
        override fun ColumnScope.Settlement() {
            result?.let {
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = "本次答案", modifier = Modifier.idText())
                }
                FlowersOrderText(inputState.text, it)
            }
        }

        @Composable
        override fun Floating() { }
    }

    override val gamePlayer: (BasicScreen) -> PlayGameState = ::FOPlayGameState
}