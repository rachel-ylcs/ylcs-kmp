package love.yinlin.ui.component.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RachelButton(
	text: String,
	icon: ImageVector? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	TextButton(
		modifier = modifier,
		enabled = enabled,
		onClick = onClick
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(5.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			icon?.let {
				Icon(
					imageVector = it,
					contentDescription = null
				)
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