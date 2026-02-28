package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrcodeScanner(
    modifier: Modifier = Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit
)