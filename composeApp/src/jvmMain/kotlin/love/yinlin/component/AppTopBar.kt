package love.yinlin.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.ui.component.ClickIcon
import love.yinlin.ui.component.MiniImage
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.app_name
import ylcs_kmp.composeapp.generated.resources.img_logo

@Composable
fun AppTopBar(
	modifier: Modifier = Modifier,
	onMinimized: () -> Unit,
	onClosed: () -> Unit
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
					style = MaterialTheme.typography.headlineMedium
				)
			}
		},
		actions = {
			Row(
				modifier = Modifier.padding(horizontal = 10.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				ClickIcon(
					imageVector = Icons.Filled.Remove,
					color = MaterialTheme.colorScheme.onPrimary,
					onClick = onMinimized
				)
				ClickIcon(
					imageVector = Icons.Filled.Close,
					color = MaterialTheme.colorScheme.onPrimary,
					onClick = onClosed
				)
			}
		}
	)
}