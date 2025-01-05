package love.yinlin.component

import androidx.compose.foundation.layout.*
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
			ClickIconRow(
				color = MaterialTheme.colorScheme.onSecondaryContainer,
				items = listOf(
					Icons.Filled.Remove to onMinimized,
					Icons.Filled.Close to onClosed
				)
			)
		}
	)
}