package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun QrcodeBox(
    text: String,
    modifier: Modifier = Modifier,
    logo: DrawableResource? = null,
) {
    UnsupportedPlatformComponent(modifier = modifier)
}