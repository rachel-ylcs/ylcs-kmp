package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager

//@Stable
//class Track(
//    val left: Offset,
//    val right: Offset
//) {
//    companion object {
//        // 顶点
//        val vertices = Offset(960f, 0f)
//    }
//}
//
//@Stable
//class TrackUI(
//    rhymeManager: RhymeManager,
//) : Spirit(rhymeManager), BoxBody, Dynamic {
//    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -216f))
//    override val size: Size = Size(1920f, 1296f)
//
//    override fun onUpdate(tick: Long) {
//
//    }
//
//    override fun Drawer.onDraw() {
//
//    }
//}