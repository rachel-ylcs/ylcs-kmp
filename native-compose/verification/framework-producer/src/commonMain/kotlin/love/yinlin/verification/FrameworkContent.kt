package love.yinlin.verification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Density
import love.yinlin.verification.resources.Res

/** The framework exports ordinary Compose types through its usual Maven coordinates. */
@Composable
fun FrameworkContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { children, constraints ->
        val placeables = children.map { it.measure(constraints) }
        layout(constraints.minWidth, constraints.minHeight) { placeables.forEach { it.place(0, 0) } }
    }
}

fun frameworkDensity(density: Density): Float = density.density * density.fontScale

suspend fun readFrameworkResource(): String = Res.readBytes("files/library 中文.txt").decodeToString().trim()
