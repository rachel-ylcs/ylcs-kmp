package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class MissEnvironment(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val size: Size = manager.size

    override fun Drawer.onClientDraw() {

    }
}