package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.extension.toJson
import love.yinlin.ui.component.container.BlockCharacter
import love.yinlin.ui.component.container.CharacterBlock
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.screen.SubScreenSlot

@Composable
fun ColumnScope.BlockTextCardInfo(game: GamePublicDetails) {}

@Stable
class BlockTextCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = BTConfig

    private var blockSize by mutableFloatStateOf(0f)
    private val gridSize by derivedStateOf { blockSize.cast(BTConfig.minBlockSize, BTConfig.maxBlockSize) }
    private val data = List(BTConfig.maxBlockSize * BTConfig.maxBlockSize) { BlockCharacter.Empty }.toMutableStateList()

    override val canSubmit: Boolean by derivedStateOf {
        var anyHide = false
        var anyBlock = false
        data.fastForEach { it.decode { ch, hide ->
            if (hide) anyHide = true
            if (ch == BTConfig.CHAR_BLOCK) anyBlock = true
        } }
        anyHide && !anyBlock
    }

    override val submitInfo: JsonElement = Unit.toJson()

    override val submitQuestion: JsonElement get() = JsonPrimitive(buildString(gridSize * gridSize) {
        data.take(gridSize * gridSize).fastForEach {
            append(it.decode { ch, hide -> if (hide) BTConfig.CHAR_BLOCK else ch })
        }
    })

    override val submitAnswer: JsonElement get() = JsonPrimitive(buildString(gridSize * gridSize) {
        data.take(gridSize * gridSize).fastForEach {
            append(it.ch)
        }
    })

    @Composable
    override fun ColumnScope.Content() {
        GameSlider(
            title = "网格大小",
            progress = blockSize,
            minValue = BTConfig.minBlockSize,
            maxValue = BTConfig.maxBlockSize,
            onProgressChange = { blockSize = it },
            modifier = Modifier.fillMaxWidth()
        )
        CharacterBlock(
            blockSize = gridSize,
            data = data,
            writeMode = true,
            onCharacterSelected = { _, ch ->
                characterInputDialog.openSuspend(ch?.toString() ?: "")?.firstOrNull()
            },
            onCharacterChanged = { index, ch -> data[index] = ch },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )
    }

    private val characterInputDialog = FloatingDialogInput(
        hint = "输入文字",
        maxLength = 1,
        clearButton = false
    )

    @Composable
    override fun Floating() {
        characterInputDialog.Land()
    }
}

@Stable
class BlockTextPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    override val config = BTConfig

    override val canSubmit: Boolean = false

    override val submitAnswer: JsonElement = JsonNull

    @Composable
    override fun Content(preflightResult: PreflightResult) {

    }

    @Composable
    override fun ColumnScope.Settlement(gameResult: GameResult) {

    }
}