package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Container
import love.yinlin.compose.game.traits.Soul
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
private class LeftUI(
    rhymeManager: RhymeManager,
    recordImage: ImageBitmap
) : Container(rhymeManager), BoxBody {
    override val size: Size = Size(700f, 200f)

    override val souls: List<Soul> = listOf(
        RecordContainer(rhymeManager, recordImage),
        ProgressBar(rhymeManager),
        ScoreBoard(rhymeManager)
    )

    private val backgorund = manager.assets.image("left_ui")!!.image

    override fun Drawer.onClientPreDraw() {
        image(backgorund)
    }
}

@Stable
private class RightUI(
    rhymeManager: RhymeManager,
    lyrics: RhymeLyricsConfig
) : Container(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1220f, 0f))
    override val size: Size = Size(700f, 100f)

    override val souls: List<Soul> = listOf(
        LyricsBar(rhymeManager, lyrics)
    )

    private val backgorund = manager.assets.image("right_ui")!!.image

    override fun Drawer.onClientPreDraw() {
        image(backgorund)
    }
}

@Stable
class Scene(
    rhymeManager: RhymeManager,
    lyrics: RhymeLyricsConfig,
    recordImage: ImageBitmap,
) : Container(rhymeManager), BoxBody {
    override val size: Size = manager.size

    override val souls: List<Soul> = listOf(
        LeftUI(rhymeManager, recordImage),
        RightUI(rhymeManager, lyrics),
        ComboBoard(rhymeManager),
        TrackUI(rhymeManager, lyrics)
    )
}