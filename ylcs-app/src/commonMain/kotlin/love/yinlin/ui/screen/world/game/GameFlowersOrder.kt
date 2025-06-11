package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.info.FOConfig
import love.yinlin.extension.toJson
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot

@Stable
class FlowersOrderCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = FOConfig

    private var tryCount by mutableFloatStateOf(0f)
    private val content = TextInputState()

    override val canSubmit: Boolean by derivedStateOf {
        val text = content.text
        text.length in FOConfig.minLength .. FOConfig.maxLength && text.all { it.code !in 0 .. 127 }
    }

    override val submitInfo: JsonElement = Unit.toJson()

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