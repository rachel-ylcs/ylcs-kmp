package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.LocalAppContext
import love.yinlin.data.TabItem
import love.yinlin.model.AppModel
import love.yinlin.ui.NavigationItem

@Composable
private fun ScreenMainContent(
	model: AppModel,
	navController: NavController,
	pagerState: PagerState,
	innerPadding: PaddingValues
) {
	HorizontalPager(
		state = pagerState,
		modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding),
		beyondViewportPageCount = TabItem.entries.size
	) {
		when (it) {
			TabItem.WORLD.ordinal -> ScreenWorld(model, navController)
			TabItem.MSG.ordinal -> ScreenMsg(model, navController)
			TabItem.MUSIC.ordinal -> ScreenMusic(model, navController)
			TabItem.DISCOVERY.ordinal -> ScreenDiscovery(model, navController)
			TabItem.ME.ordinal -> ScreenMe(model, navController)
		}
	}
}

@Composable
private fun Portrait(
	model: AppModel,
	navController: NavController,
	pagerState: PagerState,
	coroutineScope: CoroutineScope
) {
	Scaffold(
		modifier = Modifier.fillMaxSize(),
		bottomBar = {
			NavigationBar(
				modifier = Modifier.fillMaxWidth().height(65.dp),
				containerColor = MaterialTheme.colorScheme.surface,
			) {
				TabItem.entries.forEachIndexed { index, tabItem ->
					NavigationItem(
						item = tabItem,
						isSelected = index == pagerState.currentPage,
						rowScope = this
					) {
						coroutineScope.launch {
							pagerState.animateScrollToPage(index)
						}
					}
				}
			}
		}
	) {
		ScreenMainContent(model, navController, pagerState, it)
	}
}

@Composable
private fun Landscape(
	model: AppModel,
	navController: NavController,
	pagerState: PagerState,
	coroutineScope: CoroutineScope
) {
	Row(modifier = Modifier.fillMaxSize()) {
		NavigationRail(
			modifier = Modifier.fillMaxHeight().width(70.dp),
			containerColor = MaterialTheme.colorScheme.surface
		) {
			TabItem.entries.forEachIndexed { index, tabItem ->
				NavigationItem(
					item = tabItem,
					isSelected = index == pagerState.currentPage,
					rowScope = null
				) {
					coroutineScope.launch {
						pagerState.animateScrollToPage(index)
					}
				}
			}
		}
		Scaffold(modifier = Modifier.weight(1f)) {
			ScreenMainContent(model, navController, pagerState, it)
		}
	}
}

@Composable
fun ScreenMain(
	model: AppModel,
	navController: NavController
) {
	val pagerState = rememberPagerState(0) { TabItem.entries.size }
	val coroutineScope = rememberCoroutineScope()
	if (LocalAppContext.current.isPortrait) Portrait(model, navController, pagerState, coroutineScope)
	else Landscape(model, navController, pagerState, coroutineScope)
}