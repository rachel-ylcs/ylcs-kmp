package love.yinlin.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal expect val platformViewName: String?

@Composable
internal expect fun PlatformViewExample(text: String, modifier: Modifier)
