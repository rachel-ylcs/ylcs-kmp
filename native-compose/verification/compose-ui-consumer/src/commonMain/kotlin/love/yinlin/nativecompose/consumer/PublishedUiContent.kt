package love.yinlin.nativecompose.consumer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout

/** A real commonMain consumer, compiled with official or explicit Native Maven dependencies. */
@Composable
internal fun PublishedUiContent(color: Color, onDispose: () -> Unit) {
    DisposableEffect(Unit) { onDispose { onDispose() } }
    Layout(content = {}, modifier = Modifier.drawBehind { drawRect(color) }) { _, _ -> layout(32, 24) {} }
}
