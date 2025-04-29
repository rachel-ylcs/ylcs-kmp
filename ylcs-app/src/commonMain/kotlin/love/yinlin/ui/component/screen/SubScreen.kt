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
import love.yinlin.AppModel
import love.yinlin.common.LocalOrientation
import love.yinlin.common.Orientation
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.layout.SplitActionLayout
import love.yinlin.ui.screen.Screen

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
abstract class SubScreen<A>(model: AppModel) : Screen<A>(model) {
	protected abstract val title: String

	protected open fun onBack() { pop() }

	@Composable
	protected open fun ActionScope.LeftActions() { }

	@Composable
	protected open fun ActionScope.RightActions() { }

	@Composable
	protected open fun BottomBar() { }

	@Composable
	protected abstract fun SubContent(orientation: Orientation)

	@OptIn(ExperimentalComposeUiApi::class)
    @Composable
	final override fun Content() {
		BackHandler { onBack() }

		Scaffold(modifier = Modifier.fillMaxSize()) {
			Column(modifier = Modifier.fillMaxSize().padding(it)) {
				Surface(
					modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
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
							Text(
								text = title,
								style = MaterialTheme.typography.titleMedium,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
						}
						SplitActionLayout(
							modifier = Modifier.fillMaxWidth().zIndex(5f),
							left = {
								ClickIcon(
									modifier = Modifier.padding(start = 10.dp),
									icon = Icons.AutoMirrored.Outlined.ArrowBack,
									onClick = ::onBack
								)
								LeftActions()
							},
							right = {
								RightActions()
							}
						)
					}
				}
				Box(
					modifier = Modifier.fillMaxWidth().weight(1f)
						.background(MaterialTheme.colorScheme.background)
				) {
					SubContent(LocalOrientation.current)
				}
				Surface(
					modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
					tonalElevation = 1.dp,
					shadowElevation = 5.dp
				) {
					BottomBar()
				}
			}
		}
	}
}

@Stable
abstract class CommonSubScreen(model: AppModel) : SubScreen<Unit>(model)