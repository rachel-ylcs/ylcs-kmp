package love.yinlin.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.ThemeMode
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.Space
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import love.yinlin.resources.*
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.ui.component.layout.equalItem
import love.yinlin.ui.component.screen.Tip

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
			.width(IntrinsicSize.Min)
			.clip(MaterialTheme.shapes.medium)
			.clickable(onClick = onClick)
			.padding(horizontal = 10.dp, vertical = 5.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		MiniImage(
			res = if (isSelected) tabItem.iconActive else tabItem.iconNormal,
			modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(5.dp)
		)
		Text(
			text = stringResource(tabItem.title),
			style = MaterialTheme.typography.labelMedium,
			color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
		tonalElevation = 1.dp,
		shadowElevation = 5.dp
	) {
		EqualRow(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
			for (index in TabItem.entries.indices) {
				equalItem {
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
		tonalElevation = 1.dp,
		shadowElevation = 5.dp
	) {
		Column(
			modifier = Modifier.fillMaxHeight().padding(horizontal = 5.dp, vertical = 10.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			ClickIcon(
				icon = when (app.theme) {
					ThemeMode.SYSTEM -> Icons.Filled.Contrast
					ThemeMode.LIGHT -> Icons.Filled.LightMode
					ThemeMode.DARK -> Icons.Filled.DarkMode
				},
				color = MaterialTheme.colorScheme.primary,
				onClick = {
					app.theme = app.theme.next
				}
			)
			Space(10.dp)
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
class ScreenMain(model: AppModel) : Screen<ScreenMain.Args>(model) {
	@Stable
	@Serializable
	data object Args : Screen.Args

	private val pagerState = object : PagerState() {
		override val pageCount: Int = TabItem.entries.size
	}

	@Composable
	private fun PageContent(modifier: Modifier = Modifier) {
		HorizontalPager (
			state = pagerState,
			modifier = modifier
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				when (it) {
					TabItem.WORLD.ordinal -> worldPart.PartContent()
					TabItem.MSG.ordinal -> msgPart.PartContent()
					TabItem.MUSIC.ordinal -> musicPart.PartContent()
					TabItem.DISCOVERY.ordinal -> discoveryPart.PartContent()
					TabItem.ME.ordinal -> mePart.PartContent()
				}
			}
		}
	}

	@Composable
	private fun Portrait(modifier: Modifier = Modifier) {
		Scaffold(modifier = modifier) {
			Column(modifier = Modifier.fillMaxSize().padding(it)) {
				PageContent(modifier = Modifier.fillMaxWidth().weight(1f))
				PortraitNavigation(
					modifier = Modifier.fillMaxWidth().zIndex(5f),
					currentPage = pagerState.currentPage,
					onNavigate = { index ->
						launch { pagerState.scrollToPage(index) }
					}
				)
			}
		}
	}

	@Composable
	private fun Landscape(modifier: Modifier = Modifier) {
		Scaffold(modifier = modifier) {
			Row(modifier = Modifier.fillMaxSize().padding(it)) {
				LandscapeNavigation(
					modifier = Modifier.fillMaxHeight().zIndex(5f),
					currentPage = pagerState.currentPage,
					onNavigate = { index ->
						launch { pagerState.scrollToPage(index) }
					}
				)
				PageContent(modifier = Modifier.weight(1f).fillMaxHeight())
			}
		}
	}

	override suspend fun initialize() {
		mePart.updateUserToken()
	}

	@Composable
	override fun Content() {
		if (app.isPortrait) Portrait(modifier = Modifier.fillMaxSize())
		else Landscape(modifier = Modifier.fillMaxSize())

		with(model.slot) {
			Tip(state = tip)
			info.WithOpen()
			confirm.WithOpen()
			loading.WithOpen()
		}
	}
}