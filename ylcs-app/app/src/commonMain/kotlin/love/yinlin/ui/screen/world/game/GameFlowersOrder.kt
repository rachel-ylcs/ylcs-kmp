package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.compose.*
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.rachel.game.GameConfig
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
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.SubScreenSlot
import kotlin.to

@Composable
private fun FlowersOrderText(
    text: String,
    key: Int,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    modifier: Modifier = Modifier
) {
    val items = remember(key) { FOType.decode(key) }
    if (text.length == items.size) {
        Text(
            text = remember(text, items) { buildAnnotatedString {
                text.forEachIndexed { index, ch ->
                    when (items[index]) {
                        FOType.CORRECT -> withStyle(SpanStyle(color = Colors.Green4)) { append(ch) }
                        FOType.INVALID_POS -> withStyle(SpanStyle(color = Colors.Yellow4)) { append(ch) }
                        FOType.INCORRECT -> withStyle(SpanStyle(color = Colors.Red4)) { append(ch) }
                    }
                }
            } },
            style = style,
            modifier = modifier
        )
    }
}

@Composable
fun ColumnScope.FlowersOrderCardInfo(game: GamePublicDetailsWithName) {
    val info = remember(game) {
        catchingNull { game.info.to<FOInfo>() }
    }
    if (info != null) {
        NormalText(
            text = "重试次数: ${info.tryCount}",
            icon = Icons.Outlined.RestartAlt
        )
    }
}

@Composable
fun ColumnScope.FlowersOrderCardQuestionAnswer(game: GameDetailsWithName) {
    val answer = remember(game) {
        catchingNull { game.answer.String }
    }
    if (answer != null) {
        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        NormalText(
            text = answer,
            icon = Icons.Outlined.Lightbulb,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ColumnScope.FlowersOrderRecordResult(text: String, result: Int) {
    Text(
        text = "提示",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
    FlowersOrderText(
        text = text,
        key = result
    )
}

@Composable
fun ColumnScope.FlowersOrderRecordCard(answer: JsonElement, info: JsonElement) {
    val data = remember(answer, info) {
        catchingNull { answer.String to info.Int }
    }

    data?.let { (actualAnswer, actualResult) ->
        FlowersOrderRecordResult(actualAnswer, actualResult)
    }
}

@Stable
class FlowersOrderCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = FOConfig

    private var tryCount by mutableFloatStateOf(0f)
    private val content = TextInputState()

    override val canSubmit: Boolean by derivedStateOf {
        val text = content.text
        text.length in FOConfig.minLength .. FOConfig.maxLength && text.all { FOType.check(it) }
    }

    override val submitInfo: JsonElement get() = FOInfo(tryCount.cast(FOConfig.minTryCount, FOConfig.maxTryCount)).toJson()

    override val submitQuestion: JsonElement get() = JsonPrimitive(content.text.length)

    override val submitAnswer: JsonElement get() = JsonPrimitive(content.text)

    @Composable
    override fun ColumnScope.Content() {
        GameSlider(
            title = "尝试次数",
            progress = tryCount,
            minValue = FOConfig.minTryCount,
            maxValue = FOConfig.maxTryCount,
            onProgressChange = { tryCount = it },
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = content,
            hint = "内容(长度${FOConfig.minLength}~${FOConfig.maxLength})",
            maxLength = FOConfig.maxLength,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Stable
class FlowersOrderPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(
        val length: Int,
        val answer: List<String>,
        val result: List<Int>,
        val oldCharacters: AnnotatedString
    )

    companion object {
        private val DICTIONARY = """
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
    }

    override val config = FOConfig

    private var preflight: Preflight? by mutableRefStateOf(null)
    private var result: Int? by mutableRefStateOf(null)

    private val inputState = TextInputState()

    override val canSubmit: Boolean by derivedStateOf { inputState.text.length == preflight?.length }

    override val submitAnswer: JsonElement get() = JsonPrimitive(inputState.text)

    override fun init(scope: CoroutineScope, preflightResult: PreflightResult) {
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
                    for (ch in DICTIONARY) {
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

            Text(
                text = "寻花令长度: $question",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = inputState,
                hint = "答案",
                maxLength = question,
                clearButton = false,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space()
            if (answer.size == result.size) {
                repeat(answer.size) { index ->
                    FlowersOrderText(
                        text = answer[index],
                        key = result[index]
                    )
                }
            }
            Space()

            Text(
                text = "常用字表",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Space()
            Text(
                text = oldCharacters,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Space()
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {
        result?.let { FlowersOrderRecordResult(inputState.text, it) }
    }
}