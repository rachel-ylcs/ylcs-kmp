package love.yinlin.compose.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.LocalColor

@Stable
interface IndeterminateLoadingAnimation {
    @Composable
    fun Content(color: Color = LocalColor.current, modifier: Modifier = Modifier)
}