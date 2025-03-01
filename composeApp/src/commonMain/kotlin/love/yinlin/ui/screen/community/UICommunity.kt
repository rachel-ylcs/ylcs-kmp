package love.yinlin.ui.screen.community

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxText(
	text: String,
	color: Color
) {
	Box(
		modifier = Modifier.padding(vertical = 3.dp).border(1.dp, color = color),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium,
			color = color,
			modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
		)
	}
}