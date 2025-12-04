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
    scoreBoard: ScoreBoard,
    recordImage: ImageBitmap
) : Container(rhymeManager), BoxBody {
    override val size: Size = Size(700f, 200f)

    override val souls: List<Soul> = listOf(
        RecordContainer(rhymeManager, recordImage),
        ProgressBar(rhymeManager),
        scoreBoard
    )

    private val leftUIBackground: ImageBitmap by manager.assets()

    override fun Drawer.onClientPreDraw() {
        image(leftUIBackground)
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

    private val rightUIBackground: ImageBitmap by manager.assets()

    override fun Drawer.onClientPreDraw() {
        image(rightUIBackground)
    }
}

@Stable
class Scene(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
    recordImage: ImageBitmap,
) : Container(rhymeManager), BoxBody {
    override val size: Size = manager.size

    private val scoreBoard = ScoreBoard(rhymeManager)
    private val comboBoard = ComboBoard(rhymeManager)
    private val trackMap = TrackMap(rhymeManager)
    private val screenEnvironment = ScreenEnvironment(rhymeManager)

    override val souls: List<Soul> = listOf(
        trackMap,
        NoteQueue(rhymeManager, lyricsConfig, scoreBoard, comboBoard, trackMap, screenEnvironment),
        comboBoard,
        screenEnvironment,
        LeftUI(rhymeManager, scoreBoard, recordImage),
        RightUI(rhymeManager, lyricsConfig),
    )
}