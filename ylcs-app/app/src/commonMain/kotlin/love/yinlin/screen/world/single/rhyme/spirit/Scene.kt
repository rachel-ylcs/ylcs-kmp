package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Pointer
import love.yinlin.compose.game.Spirit
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Trigger
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class Scene(
    rhymeManager: RhymeManager,
    lyrics: RhymeLyricsConfig,
    recordImage: ImageBitmap,
) : Spirit(rhymeManager), BoxBody, Dynamic, Trigger {
    override val size: Size = manager.size

    private val leftUI = LeftUI(rhymeManager, recordImage)

    override fun onUpdate(tick: Long) {
        leftUI.onUpdate(tick)
    }

    override fun onEvent(pointer: Pointer) = leftUI.handle(pointer)

    override fun Drawer.onDraw() {
        leftUI.apply { draw() }
    }
}