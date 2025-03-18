package love.yinlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yinlin.common.RachelTheme
import love.yinlin.ui.component.button.loadingButton
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun preview() {
	RachelTheme(darkMode = false) {
		Box(modifier = Modifier.width(200.dp).height(400.dp)) {
			loadingButton(
				text = "登录",
				icon = Icons.Default.Download,
				onClick = {
					delay(2000L)
				}
			)
		}
	}
}