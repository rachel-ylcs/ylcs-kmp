package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.SoulContainer
import love.yinlin.compose.game.traits.Soul
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager
import love.yinlin.screen.world.single.rhyme.RhymePlayConfig

@Stable
private class LeftUI(
    rhymeManager: RhymeManager,
    playConfig: RhymePlayConfig,
    private val name: String,
    scoreBoard: ScoreBoard
) : SoulContainer(rhymeManager), BoxBody {
    override val size: Size = Size(700f, 200f)

    override val souls: List<Soul> = listOf(
        RecordContainer(rhymeManager),
        ProgressBar(rhymeManager),
        scoreBoard
    )

    private val leftUIBackground= rhymeManager.assets.leftUIBackground()
    private val difficultyStar = rhymeManager.assets.difficultyStar()

    private val difficulty = playConfig.difficulty

    private val textCache = TextDrawer.Cache()

    override fun Drawer.onClientPreDraw() {
        // 画背景
        image(leftUIBackground)

        // 画标题
        val content = measureText(textCache, name, 38f, FontWeight.Bold)
        text(
            content = content,
            color = Colors.Ghost.copy(alpha = 0.8f),
            position = Offset(250f, 10f),
            shadow = Shadow(Colors.Dark, Offset(1f, 1f), 1f)
        )

        // 画难度星级
        repeat(difficulty.ordinal + 1) { index ->
            image(difficultyStar, Offset(250f + index * 40f, 54f), Size(32f, 32f))
        }
    }
}

@Stable
private class RightUI(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig
) : SoulContainer(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1220f, 0f))
    override val size: Size = Size(700f, 100f)

    override val souls: List<Soul> = listOf(
        LyricsBar(rhymeManager, lyricsConfig)
    )

    private val rightUIBackground = rhymeManager.assets.rightUIBackground()

    override fun Drawer.onClientPreDraw() {
        image(rightUIBackground)
    }
}

@Stable
class Scene(
    rhymeManager: RhymeManager,
    playConfig: RhymePlayConfig,
    name: String,
    lyricsConfig: RhymeLyricsConfig,
) : SoulContainer(rhymeManager), BoxBody {
    override val size: Size = manager.size

    private val scoreBoard = ScoreBoard(rhymeManager)
    private val comboBoard = ComboBoard(rhymeManager)
    private val trackMap = TrackMap(rhymeManager)
    private val screenEnvironment = ScreenEnvironment(rhymeManager, lyricsConfig)

    override val souls: List<Soul> = listOf(
        trackMap,
        NoteQueue(rhymeManager, lyricsConfig, scoreBoard, comboBoard, trackMap, screenEnvironment),
        comboBoard,
        screenEnvironment,
        LeftUI(rhymeManager, playConfig, name, scoreBoard),
        RightUI(rhymeManager, lyricsConfig),
    )
}