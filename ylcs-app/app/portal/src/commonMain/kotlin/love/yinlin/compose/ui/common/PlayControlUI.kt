package love.yinlin.compose.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.node.fastClipCircle
import love.yinlin.compose.ui.node.fastOffsetX

@Composable
fun PlayControlUI(
    size: Dp,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlay: () -> Unit
) {
    val darkMode = Theme.darkMode
    val primaryColor = if (darkMode) Colors.Green5 else Colors.Steel5
    val secondaryColor = if (darkMode) Colors.Green2 else Colors.Steel3

    LoadingIcon(
        modifier = Modifier.size(size),
        icon = Icons.GotoPrevious,
        color = secondaryColor,
        onClick = onPrevious
    )
    Box(
        modifier = Modifier.wrapContentSize()
            .fastClipCircle()
            .background(primaryColor).clickable(onClick = onPlay).padding(Theme.padding.e),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon = if (isPlaying) Icons.Pause else Icons.Play,
            color = Colors.Ghost,
            modifier = Modifier.size(size).fastOffsetX { if (isPlaying) null else 1.5.dp.toPx() }
        )
    }
    LoadingIcon(
        modifier = Modifier.size(size),
        icon = Icons.GotoNext,
        color = secondaryColor,
        onClick = onNext
    )
}