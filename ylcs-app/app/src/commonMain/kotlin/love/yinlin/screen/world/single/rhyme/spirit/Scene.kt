package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.BoxBody
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.Spirit
import love.yinlin.data.music.RhymeLyricsConfig

class Scene(
    manager: Manager,
    lyrics: RhymeLyricsConfig,
    record: ImageBitmap,
) : Spirit(manager) {
    override val box = BoxBody(Offset.Zero, manager.size)

    private val leftUI = LeftUI(manager, record)

    override fun Drawer.onDraw() {
        leftUI.apply { draw() }
    }
}