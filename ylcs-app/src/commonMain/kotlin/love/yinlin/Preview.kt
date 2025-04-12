package love.yinlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.common.RachelTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun preview() {
	RachelTheme(darkMode = false) {
		Box(modifier = Modifier.width(200.dp).height(400.dp)) {

		}
	}
}