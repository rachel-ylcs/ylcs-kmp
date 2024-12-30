package love.yinlin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.app_name
import ylcs_kmp.composeapp.generated.resources.img_logo

@Composable
fun AppTopBar(
	onMinimized: () -> Unit,
	onClosed: () -> Unit
) {
	TopAppBar(
		modifier = Modifier.fillMaxWidth().height(32.dp),
		backgroundColor = MaterialTheme.colorScheme.primary,
		title = {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Image(
					painter = painterResource(Res.drawable.img_logo),
					contentDescription = null,
					modifier = Modifier.size(24.dp),
				)
				Spacer(Modifier.width(15.dp))
				Text(
					text = stringResource(Res.string.app_name),
					color = MaterialTheme.colorScheme.onSecondaryContainer,
					style = MaterialTheme.typography.headlineMedium
				)
			}
		},
		actions = {
			Icon(
				imageVector = Icons.Filled.Remove,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSecondaryContainer,
				modifier = Modifier.size(20.dp).clickable(onClick = onMinimized)
			)
			Spacer(Modifier.width(10.dp))
			Icon(
				imageVector = Icons.Filled.Close,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSecondaryContainer,
				modifier = Modifier.size(20.dp).clickable(onClick = onClosed)
			)
			Spacer(Modifier.width(10.dp))
		}
	)
}