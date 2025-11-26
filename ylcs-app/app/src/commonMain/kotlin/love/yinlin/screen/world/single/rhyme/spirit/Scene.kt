package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Container
import love.yinlin.compose.game.traits.Soul
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

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
        TrackUI(rhymeManager, lyrics)
    )
}