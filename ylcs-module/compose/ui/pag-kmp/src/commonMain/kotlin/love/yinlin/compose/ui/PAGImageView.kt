package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
expect fun PAGImageView(
    composition: PAGComposition? = null,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    config: PAGConfig = remember { PAGConfig() }
)