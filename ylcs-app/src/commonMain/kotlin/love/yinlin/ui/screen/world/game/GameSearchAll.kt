package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.info.SAConfig
import love.yinlin.data.rachel.game.info.SAInfo
import love.yinlin.extension.toJson
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot
import love.yinlin.ui.screen.community.BoxText

@Stable
class SearchAllCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = SAConfig

    private var threshold by mutableFloatStateOf(0f)
    private var timeLimit by mutableFloatStateOf(0f)
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
            title = "成功阈值",
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