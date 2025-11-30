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

    private val background = manager.assets.image("left_ui")

    override fun Drawer.onClientPreDraw() {
        image(background)
    }
}

@Stable
private class RightUI(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig
) : Container(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1220f, 0f))
    override val size: Size = Size(700f, 100f)

    override val souls: List<Soul> = listOf(
        LyricsBar(rhymeManager, lyricsConfig)
    )

    private val background = manager.assets.image("right_ui")

    override fun Drawer.onClientPreDraw() {
        image(background)
    }
}

@Stable
class Scene(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
    recordImage: ImageBitmap,
) : Container(rhymeManager), BoxBody {
    override val size: Size = manager.size

    private val trackMap = TrackMap(rhymeManager)

    override val souls: List<Soul> = listOf(
        LeftUI(rhymeManager, recordImage),
        RightUI(rhymeManager, lyricsConfig),
        ComboBoard(rhymeManager),
        trackMap,
        NoteQueue(rhymeManager, lyricsConfig, trackMap),
        ScreenEnvironment(rhymeManager),
    )
}