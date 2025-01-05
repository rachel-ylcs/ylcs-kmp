package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import love.yinlin.app
import love.yinlin.component.MiniImage
import love.yinlin.data.item.TabItem
import love.yinlin.extension.shadowTop
import love.yinlin.model.AppModel
import org.jetbrains.compose.resources.stringResource

@Composable
private fun NavigationItemText(
	item: TabItem,
	selected: Boolean
) {
	Text(
		text = stringResource(item.title),
		style = MaterialTheme.typography.titleMedium,
		color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
	)
}

@Composable
private fun PagerScope.PageContent(
	model: AppModel,
	index: Int
) {
	Box(modifier = Modifier.fillMaxSize()) {
		when (index) {
			TabItem.WORLD.ordinal -> ScreenWorld(model)
			TabItem.MSG.ordinal -> ScreenMsg(model)
			TabItem.MUSIC.ordinal -> ScreenMusic(model)
			TabItem.DISCOVERY.ordinal -> ScreenDiscovery(model)
			TabItem.ME.ordinal -> ScreenMe(model)
		}
	}
}

@Composable
private fun Portrait(model: AppModel) {
	val pagerState = rememberPagerState(0) { TabItem.entries.size }
	val coroutineScope = rememberCoroutineScope()
	Scaffold(
		modifier = Modifier.fillMaxSize(),
		bottomBar = {
			NavigationBar(
				modifier = Modifier.fillMaxWidth().shadowTop(),
				containerColor = MaterialTheme.colorScheme.surface,
			) {
				TabItem.entries.forEachIndexed { index, tabItem ->
					val isSelected = index == pagerState.currentPage
					NavigationBarItem(
						selected = isSelected,
						icon = { MiniImage(if (isSelected) tabItem.iconActive else tabItem.iconNormal) },
						label = { NavigationItemText(tabItem, isSelected) },
						onClick = {
							coroutineScope.launch {
								pagerState.scrollToPage(index)
							}
						}
					)
				}
			}
		}
	) { padding ->
		HorizontalPager(
			state = pagerState,
			modifier = Modifier.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
				.padding(padding)
		) {
			PageContent(model, it)
		}
	}
}

@Composable
private fun Landscape(model: AppModel) {
	val pagerState = rememberPagerState(0) { TabItem.entries.size }
	val coroutineScope = rememberCoroutineScope()
	Row {
		NavigationRail(
			modifier = Modifier.fillMaxHeight(),
			containerColor = MaterialTheme.colorScheme.surface
		) {
			TabItem.entries.forEachIndexed { index, tabItem ->
				val isSelected = index == pagerState.currentPage
				NavigationRailItem(
					selected = isSelected,
					icon = { MiniImage(if (isSelected) tabItem.iconActive else tabItem.iconNormal) },
					label = { NavigationItemText(tabItem, isSelected) },
					onClick = {
						coroutineScope.launch {
							pagerState.scrollToPage(index)
						}
					}
				)
			}
		}
		Scaffold(modifier = Modifier.fillMaxHeight().weight(1f)) { padding ->
			HorizontalPager (
				state = pagerState,
				modifier = Modifier.fillMaxSize()
					.background(MaterialTheme.colorScheme.background)
					.padding(padding)
			) {
				PageContent(model, it)
			}
		}
	}
}

@Composable
fun ScreenMain(model: AppModel) {
	if (app.isPortrait) Portrait(model)
	else Landscape(model)
}