package love.yinlin.compose.game.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo

@Composable
internal fun RhymeCommonButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v5,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        contentPadding = Theme.padding.value,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon)
            SimpleClipText(text = text)
        }
    }
}

@Composable
internal fun RhymeMusicCard(
    info: MusicInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        shape = Theme.shape.v7,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
        ) {
            LocalFileImage(
                uri = info.path(app.modPath, ModResourceType.Record).path,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Theme.size.image7)
            )
            Column(modifier = Modifier.weight(1f).padding(Theme.padding.value)) {
                SimpleEllipsisText(text = info.name, style = Theme.typography.v6.bold)
            }
        }
    }
}

private val DefaultRhymeBlurStyle = HazeStyle(
    blurRadius = 10.dp,
    backgroundColor = Colors(0xDD292929),
    tint = HazeTint(Colors(0x6C292929))
)

@OptIn(ExperimentalHazeApi::class)
fun Modifier.rhymeBlurTarget(state: BlurState): Modifier = this.hazeEffect(
    state = state,
    style = DefaultRhymeBlurStyle
) {
    inputScale = HazeInputScale.Fixed(0.66667f)
}

@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun RhymeBlurSurface(
    blurState: BlurState,
    shape: Shape = Theme.shape.rectangle,
    border: Dp = Theme.border.v7,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues.Zero,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(border, Theme.color.outline, shape)
            .rhymeBlurTarget(state = blurState)
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}