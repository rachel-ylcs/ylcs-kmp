package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
sealed interface DynamicActionState {
    @Stable
    class Normal : DynamicActionState
    @Stable
    class Miss : DynamicActionState
    @Stable
    class Done : DynamicActionState
}

@Stable
sealed class DynamicAction {

}

@Stable
class NoteQueue(
    rhymeManager: RhymeManager,
    lyrics: RhymeLyricsConfig,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -1080f * Track.VERTICES_TOP_RATIO))
    override val size: Size = Size(1920f, 1080f * (1 + Track.VERTICES_TOP_RATIO))

    override fun onClientUpdate(tick: Long) {
        // 先处理音符消失

        // 再处理音符进入
    }

    override fun onClientEvent(event: Event): Boolean {
        return false
    }

    override fun Drawer.onClientDraw() {

    }
}