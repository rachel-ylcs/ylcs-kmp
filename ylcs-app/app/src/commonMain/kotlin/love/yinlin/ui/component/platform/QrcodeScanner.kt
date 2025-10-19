package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrcodeScanner(
    modifier: Modifier = Modifier,
    onResult: (String) -> Unit
)