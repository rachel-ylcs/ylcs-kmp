package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon

sealed class ActionScope(private val ltr: Boolean) {
	object Left : ActionScope(true)
	object Right : ActionScope(false)

	@Composable
	fun Action(
		icon: ImageVector,
		color: Color = MaterialTheme.colorScheme.onSurface,
		enabled: Boolean = true,
		onClick: () -> Unit
	) {
		val padding = if (ltr) 10.dp else 0.dp

		ClickIcon(
			icon = icon,
			color = color,
			enabled = enabled,
			modifier = Modifier.padding(start = padding, end = 10.dp - padding),
			onClick = onClick
		)
	}

	@Composable
	fun ActionSuspend(
		icon: ImageVector,
		color: Color = MaterialTheme.colorScheme.onSurface,
		enabled: Boolean = true,
		onClick: suspend CoroutineScope.() -> Unit
	) {
		val padding = if (ltr) 10.dp else 0.dp

		LoadingIcon(
			icon = icon,
			color = color,
			enabled = enabled,
			modifier = Modifier.padding(start = padding, end = 10.dp - padding),
			onClick = onClick
		)
	}

	@Composable
	inline fun Actions(block: @Composable ActionScope.() -> Unit) = block()
}

@Stable
class SubScreenSlot {
	val tip: TipState = TipState()
	val info: DialogInfo = DialogInfo()
	val confirm: DialogConfirm = DialogConfirm()
	val loading: DialogLoading = DialogLoading()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: @Composable () -> Unit,
	actions: @Composable (ActionScope.() -> Unit) = {},
	bottomBar: @Composable () -> Unit = {},
	onBack: (() -> Unit)? = null,
	slot: SubScreenSlot,
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
								icon = Icons.AutoMirrored.Outlined.ArrowBack,
								onClick = onBack
							)
						}
					},
					expandedHeight = 48.dp,
					actions = { ActionScope.Right.actions() }
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

	with(slot) {
		Tip(state = tip)
		info.withOpen()
		confirm.withOpen()
		loading.withOpen()
	}
}

@Composable
fun SubScreen(
	modifier: Modifier = Modifier,
	title: String = "",
	actions: @Composable (ActionScope.() -> Unit) = {},
	bottomBar: @Composable () -> Unit = {},
	onBack: () -> Unit,
	slot: SubScreenSlot,
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
		slot = slot,
		content = content
	)
}