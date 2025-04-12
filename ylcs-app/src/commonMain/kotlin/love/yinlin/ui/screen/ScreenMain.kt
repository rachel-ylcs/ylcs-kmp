package love.yinlin.ui.screen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.ThemeMode
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.community.ScreenPartDiscovery
import love.yinlin.ui.screen.community.ScreenPartMe
import love.yinlin.ui.screen.msg.ScreenPartMsg
import love.yinlin.ui.screen.music.ScreenPartMusic
import love.yinlin.ui.screen.world.ScreenPartWorld
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import love.yinlin.resources.*

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
private fun NavigationItemText(
	item: TabItem,
	selected: Boolean,
	modifier: Modifier = Modifier
) {
	Text(
		modifier = modifier,
		text = stringResource(item.title),
		style = MaterialTheme.typography.titleMedium,
		color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
	)
}

@Composable
private fun PortraitNavigation(
	currentPage: Int,
	onNavigate: (Int) -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier.zIndex(5f),
		shadowElevation = 5.dp
	) {
		NavigationBar(modifier = Modifier.fillMaxWidth()) {
			TabItem.entries.forEachIndexed { index, tabItem ->
				val isSelected = index == currentPage
				NavigationBarItem(
					selected = isSelected,
					icon = { MiniImage(if (isSelected) tabItem.iconActive else tabItem.iconNormal) },
					label = { NavigationItemText(tabItem, isSelected) },
					onClick = { onNavigate(index) }
				)
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
		modifier = modifier.zIndex(5f),
		shadowElevation = 5.dp
	) {
		NavigationRail(
			modifier = Modifier.fillMaxHeight(),
			header = {
				Space(10.dp)
				ClickIcon(
					imageVector = when (app.theme) {
						ThemeMode.SYSTEM -> Icons.Filled.Contrast
						ThemeMode.LIGHT -> Icons.Filled.LightMode
						ThemeMode.DARK -> Icons.Filled.DarkMode
					},
					color = MaterialTheme.colorScheme.primary,
					onClick = {
						app.theme = app.theme.next
					}
				)
			}
		) {
			TabItem.entries.forEachIndexed { index, tabItem ->
				val isSelected = index == currentPage
				NavigationRailItem(
					selected = isSelected,
					icon = { MiniImage(if (isSelected) tabItem.iconActive else tabItem.iconNormal) },
					label = { NavigationItemText(tabItem, isSelected) },
					onClick = { onNavigate(index) }
				)
			}
		}
	}
}

@Stable
@Serializable
data object ScreenMain : Screen<ScreenMain.Model> {
	class Model(model: AppModel) : Screen.Model(model) {
		val pagerState = object : PagerState() {
			override val pageCount: Int = TabItem.entries.size
		}

		fun onPageChanged(index: Int) {
			launch { pagerState.scrollToPage(index) }
		}

		@Composable
		private fun PageContent(modifier: Modifier = Modifier) {
			HorizontalPager (
				state = pagerState,
				modifier = modifier
			) {
				Box(modifier = Modifier.fillMaxSize()) {
					when (it) {
						TabItem.WORLD.ordinal -> part<ScreenPartWorld>().partContent()
						TabItem.MSG.ordinal -> part<ScreenPartMsg>().partContent()
						TabItem.MUSIC.ordinal -> part<ScreenPartMusic>().partContent()
						TabItem.DISCOVERY.ordinal -> part<ScreenPartDiscovery>().partContent()
						TabItem.ME.ordinal -> part<ScreenPartMe>().partContent()
					}
				}
			}
		}

		@Composable
		fun Portrait(modifier: Modifier = Modifier) {
			Scaffold(
				modifier = modifier,
				bottomBar = {
					PortraitNavigation(
						modifier = Modifier.fillMaxWidth(),
						currentPage = pagerState.currentPage,
						onNavigate = { onPageChanged(it) }
					)
				}
			) {
				PageContent(modifier = Modifier.fillMaxSize().padding(it))
			}
		}

		@Composable
		fun Landscape(modifier: Modifier = Modifier) {
			Row(modifier = modifier) {
				LandscapeNavigation(
					modifier = Modifier.fillMaxHeight(),
					currentPage = pagerState.currentPage,
					onNavigate = { onPageChanged(it) }
				)
				Scaffold(modifier = Modifier.fillMaxHeight().weight(1f)) {
					PageContent(modifier = Modifier.fillMaxSize().padding(it))
				}
			}
		}
	}

	override fun model(model: AppModel): Model = Model(model).apply {
		launch {
			model.mePart.updateUserToken()
		}
	}

	@Composable
	override fun content(model: Model) {
		if (app.isPortrait) model.Portrait(modifier = Modifier.fillMaxSize())
		else model.Landscape(modifier = Modifier.fillMaxSize())
	}
}