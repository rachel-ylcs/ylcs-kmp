package love.yinlin.ui.component.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.common.*
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.SplitActionLayout
import love.yinlin.ui.screen.Screen

@Stable
abstract class SubScreen<A>(model: AppModel) : Screen<A>(model) {
	protected abstract val title: String?

	protected open fun onBack() { pop() }

	@Composable
	protected open fun ActionScope.LeftActions() { }

	@Composable
	protected open fun ActionScope.RightActions() { }

	@Composable
	protected open fun BottomBar() { }

	@Composable
	protected abstract fun SubContent(device: Device)

	@OptIn(ExperimentalComposeUiApi::class)
    @Composable
	final override fun Content() {
		BackHandler { onBack() }

		val immersivePadding = rememberImmersivePadding()
		Column(modifier = Modifier.fillMaxSize()) {
			title?.let { titleString ->
				Surface(
					modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
					tonalElevation = ThemeValue.Shadow.Tonal,
					shadowElevation = ThemeValue.Shadow.Surface
				) {
					Box(
						modifier = Modifier
							.padding(immersivePadding.withoutBottom)
							.fillMaxWidth()
							.padding(vertical = ThemeValue.Padding.VerticalSpace),
						contentAlignment = Center
					) {
						Box(
							modifier = Modifier.fillMaxWidth().zIndex(2f),
							contentAlignment = Center
						) {
							Text(
								text = titleString,
								style = MaterialTheme.typography.titleMedium,
								maxLines = 1,
								overflow = Ellipsis
							)
						}
						SplitActionLayout(
							modifier = Modifier.fillMaxWidth().zIndex(1f),
							left = {
								ClickIcon(
									modifier = Modifier.padding(start = ThemeValue.Padding.HorizontalSpace),
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
			}
			Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
				CompositionLocalProvider(
					LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop)
				) {
					SubContent(LocalDevice.current)
				}
			}
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
				tonalElevation = ThemeValue.Shadow.Tonal,
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				CompositionLocalProvider(
					LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop)
				) {
					BottomBar()
				}
			}
		}
	}
}

@Stable
abstract class CommonSubScreen(model: AppModel) : SubScreen<Unit>(model)