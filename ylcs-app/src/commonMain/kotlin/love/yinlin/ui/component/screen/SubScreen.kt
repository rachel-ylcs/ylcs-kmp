package love.yinlin.ui.component.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
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
import love.yinlin.ui.component.layout.SplitActionLayout
import love.yinlin.ui.component.layout.SplitLayout

@Stable
sealed class ActionScope(private val ltr: Boolean) {
	@Stable
	object Left : ActionScope(true)
	@Stable
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
class SubScreenSlot(scope: CoroutineScope) {
	val tip: TipState = TipState(scope)
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
	leftActions: @Composable (ActionScope.() -> Unit) = {},
	onBack: (() -> Unit)? = null,
	slot: SubScreenSlot,
	content: @Composable () -> Unit
) {
	BackHandler { onBack?.invoke() }

	Scaffold(modifier = modifier) {
		Column(modifier = Modifier.fillMaxSize().padding(it)) {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(20f),
				tonalElevation = 1.dp,
				shadowElevation = 5.dp
			) {
				Box(
					modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
					contentAlignment = Alignment.Center
				) {
					Box(
						modifier = Modifier.fillMaxWidth().zIndex(10f),
						contentAlignment = Alignment.Center
					) {
						title()
					}
					SplitActionLayout(
						modifier = Modifier.fillMaxWidth().zIndex(5f),
						left = {
							if (onBack != null) {
								ClickIcon(
									modifier = Modifier.padding(start = 10.dp),
									icon = Icons.AutoMirrored.Outlined.ArrowBack,
									onClick = onBack
								)
							}
							leftActions()
						},
						right = actions
					)
				}
			}
			Box(
				modifier = Modifier.fillMaxWidth().weight(1f)
					.background(MaterialTheme.colorScheme.background)
			) {
				content()
			}
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(20f),
				tonalElevation = 1.dp,
				shadowElevation = 5.dp
			) {
				bottomBar()
			}
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
	leftActions: @Composable (ActionScope.() -> Unit) = {},
	onBack: () -> Unit,
	slot: SubScreenSlot,
	content: @Composable () -> Unit
) {
	SubScreen(
		modifier = modifier,
		title = {
			Text(
				text = title,
				style = MaterialTheme.typography.titleMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		},
		actions = actions,
		bottomBar = bottomBar,
		leftActions = leftActions,
		onBack = onBack,
		slot = slot,
		content = content
	)
}