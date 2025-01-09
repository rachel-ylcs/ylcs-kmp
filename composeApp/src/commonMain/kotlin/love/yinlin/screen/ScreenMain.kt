package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import love.yinlin.ThemeMode
import love.yinlin.app
import love.yinlin.component.ClickIcon
import love.yinlin.component.MiniImage
import love.yinlin.component.Space
import love.yinlin.data.item.TabItem
import love.yinlin.model.AppModel
import love.yinlin.next
import org.jetbrains.compose.resources.stringResource

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
	model: AppModel,
	pagerState: PagerState,
	modifier: Modifier = Modifier
) {
	HorizontalPager (
		state = pagerState,
		modifier = modifier
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			when (it) {
				TabItem.WORLD.ordinal -> ScreenWorld(model)
				TabItem.MSG.ordinal -> ScreenMsg(model)
				TabItem.MUSIC.ordinal -> ScreenMusic(model)
				TabItem.DISCOVERY.ordinal -> ScreenDiscovery(model)
				TabItem.ME.ordinal -> ScreenMe(model)
			}
		}
	}
}

@Composable
private fun PortraitNavigation(currentPage: Int, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
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
private fun LandscapeNavigation(currentPage: Int, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
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
private fun Portrait(model: AppModel, pagerState: PagerState, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
	Scaffold(
		modifier = modifier,
		bottomBar = {
			PortraitNavigation(
				modifier = Modifier.fillMaxWidth(),
				currentPage = pagerState.currentPage,
				onNavigate = onNavigate
			)
		}
	) {
		PageContent(
			model = model,
			pagerState = pagerState,
			modifier = Modifier.fillMaxSize().padding(it)
		)
	}
}

@Composable
private fun Landscape(model: AppModel, pagerState: PagerState, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
	Row(modifier = modifier) {
		LandscapeNavigation(
			modifier = Modifier.fillMaxHeight(),
			currentPage = pagerState.currentPage,
			onNavigate = onNavigate
		)
		Scaffold(modifier = Modifier.fillMaxHeight().weight(1f)) {
			PageContent(
				model = model,
				pagerState = pagerState,
				modifier = Modifier.fillMaxSize().padding(it)
			)
		}
	}
}

@Composable
fun ScreenMain(model: AppModel) {
	val coroutineScope = rememberCoroutineScope()
	val pagerState = rememberPagerState(0) { TabItem.entries.size }
	val onNavigate = { index: Int ->
		coroutineScope.launch {
			pagerState.scrollToPage(index)
		}
		Unit
	}
	if (app.isPortrait) Portrait(model, pagerState, onNavigate, Modifier.fillMaxSize())
	else Landscape(model, pagerState, onNavigate, Modifier.fillMaxSize())
}