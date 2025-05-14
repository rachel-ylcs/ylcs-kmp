package love.yinlin.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.layout.ActionScope
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppTopBar(
	modifier: Modifier = Modifier,
	actions: @Composable ActionScope.() -> Unit = {}
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically
	) {
		MiniIcon(res = Res.drawable.img_logo)
		Space()
		Text(
			text = stringResource(Res.string.app_name),
			color = MaterialTheme.colorScheme.onPrimaryContainer,
			style = MaterialTheme.typography.bodyLarge,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
		Space()
		Row(
			modifier = Modifier.weight(1f),
			horizontalArrangement = Arrangement.End
		) {
			ActionScope.Right.actions()
		}
	}
}