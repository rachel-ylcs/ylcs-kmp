package love.yinlin.ui.screen.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.compose.*
import love.yinlin.resources.*
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.ui.component.layout.EqualItem
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.compose.ui.floating.FABLayout
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.ui.screen.Screen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
@Serializable
private enum class TabItem(
	val title: StringResource,
	val iconNormal: DrawableResource,
	val iconActive: DrawableResource
) {
	MSG(Res.string.home_nav_msg, Res.drawable.tab_msg_normal, Res.drawable.tab_msg_active),
	WORLD(Res.string.home_nav_world, Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
	MUSIC(Res.string.home_nav_music, Res.drawable.tab_music_normal, Res.drawable.tab_music_active),
	DISCOVERY(Res.string.home_nav_discovery, Res.drawable.tab_discovery_normal, Res.drawable.tab_discovery_active),
	ME(Res.string.home_nav_me, Res.drawable.tab_me_normal, Res.drawable.tab_me_active),
}

@Composable
private fun NavigationIcon(
	current: Int,
	index: Int,
	onClick: () -> Unit
) {
	val isSelected = index == current
	val tabItem = TabItem.entries[index]
	Column(
		modifier = Modifier
			.clip(MaterialTheme.shapes.medium)
			.clickable(onClick = onClick)
			.padding(CustomTheme.padding.value),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
	) {
		MiniIcon(res = if (isSelected) tabItem.iconActive else tabItem.iconNormal)
		Text(
			text = stringResource(tabItem.title),
			style = MaterialTheme.typography.labelMedium,
			color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

@Composable
private fun PortraitNavigation(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier,
		tonalElevation = CustomTheme.shadow.tonal,
		shadowElevation = CustomTheme.shadow.surface
	) {
		EqualRow(modifier = Modifier
			.padding(LocalImmersivePadding.current)
			.fillMaxWidth()
			.padding(CustomTheme.padding.littleSpace)) {
			for (index in TabItem.entries.indices) {
				EqualItem {
					NavigationIcon(
						index = index,
						current = currentPage,
						onClick = { onNavigate(index) }
					)
				}
			}
		}
	}
}

@Composable
private fun LandscapeNavigation(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier,
		tonalElevation = CustomTheme.shadow.tonal,
		shadowElevation = CustomTheme.shadow.surface
	) {
		Column(
			modifier = Modifier
				.fillMaxHeight()
				.padding(CustomTheme.padding.littleSpace)
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.SpaceEvenly
		) {
			for (index in TabItem.entries.indices) {
				NavigationIcon(
					index = index,
					current = currentPage,
					onClick = { onNavigate(index) }
				)
			}
		}
	}
}

@Stable
class ScreenMain(model: AppModel) : Screen<Unit>(model) {
	private val pagerState = object : PagerState() {
		override val pageCount: Int = TabItem.entries.size
	}

	@Composable
	private fun PageContent(modifier: Modifier = Modifier) {
		HorizontalPager(
			state = pagerState,
			key = { TabItem.entries[it] },
			beyondViewportPageCount = 0,
			modifier = modifier
		) {
			parts.getOrNull(it)?.let { part ->
				Box(modifier = Modifier.fillMaxSize()) {
					part.Content()

					part.fabIcon?.let { icon ->
						FABLayout(
							icon = icon,
							canExpand = part.fabCanExpand,
							onClick = part::onFabClick,
							menus = part.fabMenus
						)
					}
				}
			} ?: EmptyBox()
		}

		LaunchedEffect(pagerState.settledPage) {
			parts.getOrNull(pagerState.settledPage)?.let { part ->
				part.firstLoad(update = { part.update() }) {
					launch {
						part.initialize()
						part.update()
					}
				}
			}
		}
	}

	@Composable
	private fun Portrait() {
		Column(modifier = Modifier.fillMaxSize()) {
			CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutBottom) {
				PageContent(modifier = Modifier.fillMaxWidth().weight(1f))
			}
			CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutTop) {
				PortraitNavigation(
					modifier = Modifier.fillMaxWidth().wrapContentHeight(),
					currentPage = pagerState.currentPage,
					onNavigate = { index ->
						launch { pagerState.scrollToPage(index) }
					}
				)
			}
		}
	}

	@Composable
	private fun Landscape() {
		Row(modifier = Modifier.fillMaxSize()) {
			CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutEnd) {
				LandscapeNavigation(
					modifier = Modifier.fillMaxHeight(),
					currentPage = pagerState.currentPage,
					onNavigate = { index ->
						launch { pagerState.scrollToPage(index) }
					}
				)
			}
			CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutStart) {
				PageContent(modifier = Modifier.weight(1f).fillMaxHeight())
			}
		}
	}

	override suspend fun initialize() {
		mePart.updateUserToken()
	}

	@Composable
	override fun Content() {
		val immersivePadding = rememberImmersivePadding()
		CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait()
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
            }
		}
	}

	@Composable
	override fun Floating() {
		parts.getOrNull(pagerState.settledPage)?.Floating()

		with(model.slot) {
			info.Land()
			confirm.Land()
			loading.Land()
			tip.Land()
		}
	}
}