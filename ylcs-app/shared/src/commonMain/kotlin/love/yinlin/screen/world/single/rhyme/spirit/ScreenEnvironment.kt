package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Container
import love.yinlin.compose.game.traits.Soul
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class ScreenEnvironment(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
) : Container(rhymeManager), BoxBody {
    val missEnvironment = MissEnvironment(rhymeManager)

    override val size: Size = manager.size

    override val souls: List<Soul> = listOf(
        missEnvironment,
        ChorusEnvironment(rhymeManager, lyricsConfig),
    )
}