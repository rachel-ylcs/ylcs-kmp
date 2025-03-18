package love.yinlin.ui.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.extension.rememberState
import love.yinlin.ui.component.image.DEFAULT_ICON_SIZE
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.LoadingAnimation

@Composable
fun loadingButton(
	text: String,
	icon: ImageVector? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberState { false }

	TextButton(
		modifier = modifier,
		enabled = enabled && !isLoading,
		onClick = {
			scope.launch {
				isLoading = true
				onClick()
				isLoading = false
			}
		}
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(if (isLoading) 10.dp else 5.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			if (isLoading) LoadingAnimation(size = 24.dp)
			else {
				icon?.let {
					MiniIcon(
						imageVector = it,
						size = 24.dp
					)
				}
			}
			Text(
				text = text,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}