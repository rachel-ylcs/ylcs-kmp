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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import love.yinlin.AppModel
import love.yinlin.platform.ThemeMode
import love.yinlin.platform.app
import love.yinlin.data.item.TabItem
import love.yinlin.launch
import love.yinlin.platform.next
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.screen.community.DiscoveryModel
import love.yinlin.ui.screen.community.MeModel
import love.yinlin.ui.screen.community.ScreenDiscovery
import love.yinlin.ui.screen.community.ScreenMe
import love.yinlin.ui.screen.msg.MsgModel
import love.yinlin.ui.screen.msg.ScreenMsg
import org.jetbrains.compose.resources.stringResource

class MainModel(val appModel: AppModel) {
	val pagerState = object : PagerState() {
		override val pageCount: Int = TabItem.entries.size
	}

	val msgModel = MsgModel(this)
	val discoveryModel = DiscoveryModel(this)
	val meModel = MeModel(this)

	fun <T : Any> navigate(route: T, options: NavOptions? = null, extras: Navigator.Extras? = null) = appModel.navigate(route, options, extras)
	fun pop() = appModel.pop()
	fun launch(block: suspend CoroutineScope.() -> Unit): Job = appModel.launch(block = block)

	fun onNavigate(index: Int) {
		launch { pagerState.scrollToPage(index) }
	}
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
private fun PageContent(
	model: MainModel,
	modifier: Modifier = Modifier
) {
	HorizontalPager (
		state = model.pagerState,
		modifier = modifier
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			when (it) {
				TabItem.WORLD.ordinal -> ScreenWorld(model)
				TabItem.MSG.ordinal -> ScreenMsg(model.msgModel)
				TabItem.MUSIC.ordinal -> ScreenMusic()
				TabItem.DISCOVERY.ordinal -> ScreenDiscovery(model.discoveryModel)
				TabItem.ME.ordinal -> ScreenMe(model.meModel)
			}
		}
	}
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

@Composable
private fun Portrait(
	model: MainModel,
	modifier: Modifier = Modifier
) {
	Scaffold(
		modifier = modifier,
		bottomBar = {
			PortraitNavigation(
				modifier = Modifier.fillMaxWidth(),
				currentPage = model.pagerState.currentPage,
				onNavigate = { model.onNavigate(it) }
			)
		}
	) {
		PageContent(
			model = model,
			modifier = Modifier.fillMaxSize().padding(it)
		)
	}
}

@Composable
private fun Landscape(
	model: MainModel,
	modifier: Modifier = Modifier
) {
	Row(modifier = modifier) {
		LandscapeNavigation(
			modifier = Modifier.fillMaxHeight(),
			currentPage = model.pagerState.currentPage,
			onNavigate = { model.onNavigate(it) }
		)
		Scaffold(modifier = Modifier.fillMaxHeight().weight(1f)) {
			PageContent(
				model = model,
				modifier = Modifier.fillMaxSize().padding(it)
			)
		}
	}
}

@Composable
fun ScreenMain(model: AppModel) {
	if (app.isPortrait) Portrait(
		model = model.mainModel,
		modifier = Modifier.fillMaxSize()
	)
	else Landscape(
		model = model.mainModel,
		modifier = Modifier.fillMaxSize()
	)
}