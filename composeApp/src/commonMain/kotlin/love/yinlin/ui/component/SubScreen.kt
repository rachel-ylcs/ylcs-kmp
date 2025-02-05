package love.yinlin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: @Composable () -> Unit,
	actions: @Composable (RowScope.() -> Unit) = { },
	onBack: (() -> Unit)? = null,
	isBacking: MutableState<Boolean> = remember { mutableStateOf(false) },
	content: @Composable () -> Unit
) {
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
						onBack?.let {
							ClickIcon(
								modifier = Modifier.padding(horizontal = 5.dp),
								imageVector = Icons.AutoMirrored.Filled.ArrowBack,
								onClick = {
									if (!isBacking.value) {
										isBacking.value = true
										it()
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
			content()
		}
	}
}

@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: String = "",
	actions: @Composable (RowScope.() -> Unit) = { },
	onBack: () -> Unit,
	content: @Composable () -> Unit
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