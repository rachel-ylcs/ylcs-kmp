package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.common.Colors
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.FOConfig
import love.yinlin.data.rachel.game.info.FOInfo
import love.yinlin.data.rachel.game.info.FOType
import love.yinlin.extension.Int
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot

@Composable
fun ColumnScope.FlowersOrderCardInfo(game: GamePublicDetails) {
    val info = remember(game) {
        try { game.info.to<FOInfo>() } catch (_: Throwable) { null }
    }
    if (info != null) {
        RachelText(
            text = "重试次数: ${info.tryCount}",
            icon = Icons.Outlined.RestartAlt
        )
    }
}

@Stable
class FlowersOrderCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = FOConfig

    private var tryCount by mutableFloatStateOf(0f)
    private val content = TextInputState()

    override val canSubmit: Boolean by derivedStateOf {
        val text = content.text
        text.length in FOConfig.minLength .. FOConfig.maxLength && text.all { it.code !in 0 .. 127 }
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
            hint = "内容(长度${FOConfig.minLength} - ${FOConfig.maxLength})",
            maxLength = FOConfig.maxLength,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Stable
class FlowersOrderPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val question: Int, val answer: List<String>, val result: List<Int>)

    override val config = FOConfig

    private var preflight: Preflight? by mutableStateOf(null)
    private var result: Int? by mutableStateOf(null)

    private val inputState = TextInputState()

    override val canSubmit: Boolean by derivedStateOf { inputState.text.length == preflight?.question }

    override val submitAnswer: JsonElement get() = JsonPrimitive(inputState.text)

    override fun reset() {
        inputState.text = ""
        preflight = null
        result = null
    }

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
    override fun ColumnScope.Content(preflightResult: PreflightResult) {
        LaunchedEffect(preflightResult) {
            try {
                preflight = Preflight(
                    question = preflightResult.question.Int,
                    answer = preflightResult.answer.to<List<String>>(),
                    result = preflightResult.result.to<List<GameResult>>().map { it.info.Int }
                )
            } catch (_: Throwable) { }
        }

        preflight?.let { (question, answer, result) ->
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
                modifier = Modifier.fillMaxWidth()
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
        }
    }

    @Composable
    override fun ColumnScope.Settlement(gameResult: GameResult) {
        LaunchedEffect(gameResult) {
            try {
                result = gameResult.info.Int
            } catch (_: Throwable) {}
        }

        result?.let {
            Text(
                text = "提示",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            FlowersOrderText(
                text = inputState.text,
                key = it
            )
        }
    }
}