package love.yinlin.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.resources.Res
import love.yinlin.resources.app_name
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.screen.ActionScope
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppTopBar(
	modifier: Modifier = Modifier,
	actions: @Composable ActionScope.() -> Unit = {}
) {
	TopAppBar(
		modifier = modifier,
		backgroundColor = MaterialTheme.colorScheme.primary,
		title = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(15.dp),
			) {
				MiniImage(Res.drawable.img_logo)
				Text(
					text = stringResource(Res.string.app_name),
					color = MaterialTheme.colorScheme.onSecondaryContainer,
					style = MaterialTheme.typography.headlineLarge
				)
			}
		},
		actions = { ActionScope.Right.actions() }
	)
}