package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.ui.screen.SubScreenSlot

@Stable
class BlockTextCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = BTConfig

    override val canSubmit: Boolean = true

    override val submitInfo: JsonElement = JsonNull
    override val submitQuestion: JsonElement = JsonNull
    override val submitAnswer: JsonElement = JsonNull

    @Composable
    override fun ColumnScope.Content() {

    }
}