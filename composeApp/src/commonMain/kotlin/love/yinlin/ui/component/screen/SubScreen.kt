package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.extension.rememberStateSaveable
import love.yinlin.ui.component.image.ClickIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: @Composable () -> Unit,
	actions: @Composable (RowScope.() -> Unit) = { },
	onBack: (() -> Unit)? = null,
	content: @Composable (isBacking: Boolean) -> Unit
) {
	var isBacking by rememberStateSaveable { false }
	Scaffold(
		modifier = modifier,
		topBar = {
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = 5.dp
			) {
				CenterAlignedTopAppBar(
					modifier = Modifier.fillMaxWidth(),
					title = title,
					navigationIcon = {
						if (onBack != null) {
							ClickIcon(
								modifier = Modifier.padding(horizontal = 5.dp),
								imageVector = Icons.AutoMirrored.Filled.ArrowBack,
								onClick = {
									if (!isBacking) {
										isBacking = true
										onBack()
									}
								}
							)
						}
					},
					expandedHeight = 48.dp,
					actions = actions
				)
			}
		}
	) {
		Box(modifier = Modifier.fillMaxSize()
			.padding(it)
			.background(MaterialTheme.colorScheme.background)
		) {
			content(isBacking)
		}
	}
}

@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: String = "",
	actions: @Composable (RowScope.() -> Unit) = { },
	onBack: () -> Unit,
	content: @Composable (isBacking: Boolean) -> Unit
) {
	SubScreen(
		modifier = modifier,
		title = {
			Text(
				text = title,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		},
		actions = actions,
		onBack = onBack,
		content = content
	)
}