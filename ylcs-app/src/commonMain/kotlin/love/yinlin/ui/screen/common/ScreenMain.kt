package love.yinlin.ui.screen.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.ImmersivePadding
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.resources.*
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.EqualItem
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.ui.screen.Screen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class TabItem(
	val title: StringResource,
	val iconNormal: DrawableResource,
	val iconActive: DrawableResource
) {
	WORLD(Res.string.home_nav_world, Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
	MSG(Res.string.home_nav_msg, Res.drawable.tab_msg_normal, Res.drawable.tab_msg_active),
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
			.padding(ThemeValue.Padding.Value),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
		tonalElevation = ThemeValue.Shadow.Tonal,
		shadowElevation = ThemeValue.Shadow.Surface
	) {
		EqualRow(modifier = Modifier
			.padding(LocalImmersivePadding.current)
			.fillMaxWidth()
			.padding(ThemeValue.Padding.LittleSpace)) {
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
		tonalElevation = ThemeValue.Shadow.Tonal,
		shadowElevation = ThemeValue.Shadow.Surface
	) {
		Column(
			modifier = Modifier
				.fillMaxHeight()
				.padding(ThemeValue.Padding.LittleSpace)
				.verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally
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
			modifier = modifier
		) {
			val part = when (it) {
				TabItem.WORLD.ordinal -> worldPart
				TabItem.MSG.ordinal -> msgPart
				TabItem.MUSIC.ordinal -> musicPart
				TabItem.DISCOVERY.ordinal -> discoveryPart
				TabItem.ME.ordinal -> mePart
				else -> null
			}
			if (part != null) {
				Box(modifier = Modifier.fillMaxSize()) {
					part.Content()
				}

				LaunchedEffect(part.firstLoad) {
					launch { part.initialize() }
				}
			}
			else EmptyBox()
		}
	}

	@Composable
	private fun Portrait() {
		Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
			val immersivePadding = ImmersivePadding(padding)
			Column(modifier = Modifier.fillMaxSize()) {
				CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutBottom) {
					PageContent(modifier = Modifier.fillMaxWidth().weight(1f))
				}
				CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutTop) {
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
	}

	@Composable
	private fun Landscape() {
		Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
			val immersivePadding = ImmersivePadding(padding)
			Row(modifier = Modifier.fillMaxSize()) {
				CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutEnd) {
					LandscapeNavigation(
						modifier = Modifier.fillMaxHeight(),
						currentPage = pagerState.currentPage,
						onNavigate = { index ->
							launch { pagerState.scrollToPage(index) }
						}
					)
				}
				CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutStart) {
					PageContent(modifier = Modifier.weight(1f).fillMaxHeight())
				}
			}
		}
	}

	override suspend fun initialize() {
		mePart.updateUserToken()
	}

	@Composable
	override fun Content() {
		when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> Portrait()
			Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
		}
	}

	@Composable
	override fun Floating() {
		when (pagerState.settledPage) {
			TabItem.WORLD.ordinal -> worldPart
			TabItem.MSG.ordinal -> msgPart
			TabItem.MUSIC.ordinal -> musicPart
			TabItem.DISCOVERY.ordinal -> discoveryPart
			TabItem.ME.ordinal -> mePart
			else -> null
		}?.Floating()

		with(model.slot) {
			info.Land()
			confirm.Land()
			loading.Land()
			tip.Land()
		}
	}
}