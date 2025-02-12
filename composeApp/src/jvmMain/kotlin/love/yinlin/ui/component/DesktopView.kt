package love.yinlin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.NoOpUpdate
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import love.yinlin.DesktopContext
import love.yinlin.app
import java.awt.Component

@Composable
fun <T : Component> DesktopView(
	background: Color = Color.White,
	factory: () -> T,
	modifier: Modifier = Modifier,
	update: (T) -> Unit = NoOpUpdate,
) {
	(app as DesktopContext).rawDensity?.let {
		CompositionLocalProvider(LocalDensity provides it) {
			SwingPanel(
				background = background,
				factory = factory,
				modifier = modifier,
				update = update
			)
		}
	}
}