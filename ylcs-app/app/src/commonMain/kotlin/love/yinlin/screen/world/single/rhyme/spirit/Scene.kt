package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.compose.game.AABB
import love.yinlin.compose.game.Manager
import love.yinlin.compose.game.Spirit
import love.yinlin.compose.game.TextDrawer
import love.yinlin.data.music.RhymeLyricsConfig

class Scene(
    manager: Manager,
    lyrics: RhymeLyricsConfig,
    record: ImageBitmap,
) : Spirit(manager) {
    override val box: AABB = AABB(Offset(0f, 0f), manager.size)

    override fun DrawScope.onDraw(textDrawer: TextDrawer) {

    }
}