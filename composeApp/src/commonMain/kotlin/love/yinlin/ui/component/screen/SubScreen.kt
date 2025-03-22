package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.ui.component.image.ClickIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: @Composable () -> Unit,
	actions: @Composable (RowScope.() -> Unit) = {},
	bottomBar: @Composable () -> Unit = {},
	onBack: (() -> Unit)? = null,
	tip: TipState = remember { TipState() },
	loading: DialogState = remember { DialogState() },
	content: @Composable () -> Unit
) {
	BackHandler { onBack?.invoke() }

	Scaffold(
		modifier = modifier,
		topBar = {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(520f),
				shadowElevation = 5.dp
			) {
				CenterAlignedTopAppBar(
					modifier = Modifier.fillMaxWidth(),
					title = title,
					navigationIcon = {
						if (onBack != null) {
							ClickIcon(
								modifier = Modifier.padding(horizontal = 5.dp),
								imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
								onClick = onBack
							)
						}
					},
					expandedHeight = 48.dp,
					actions = actions
				)
			}
		},
		bottomBar = {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(520f),
				shadowElevation = 5.dp
			) {
				bottomBar()
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

	Tip(state = tip)

	if (loading.isOpen) {
		DialogLoading(state = loading)
	}
}

@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: String = "",
	actions: @Composable (RowScope.() -> Unit) = {},
	bottomBar: @Composable () -> Unit = {},
	onBack: () -> Unit,
	tip: TipState = remember { TipState() },
	loading: DialogState = remember { DialogState() },
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
		bottomBar = bottomBar,
		onBack = onBack,
		tip = tip,
		loading = loading,
		content = content
	)
}