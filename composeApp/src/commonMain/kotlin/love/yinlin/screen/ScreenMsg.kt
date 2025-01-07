package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import love.yinlin.component.BoxState
import love.yinlin.component.ClickIcon
import love.yinlin.component.Segment
import love.yinlin.data.item.MsgTabItem
import love.yinlin.extension.shadowBottom
import love.yinlin.model.AppModel
import love.yinlin.platform.Coroutines

@Composable
fun ScreenMsg(model: AppModel) {
	val pagerState = rememberPagerState(0) { MsgTabItem.entries.size }
	val coroutineScope = rememberCoroutineScope()
	Column {
		Row(
			modifier = Modifier.shadowBottom()
				.background(MaterialTheme.colorScheme.background)
				.padding(horizontal = 10.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			ClickIcon(
				imageVector = Icons.Filled.Refresh,
				onClick = {
					when (pagerState.currentPage) {
						MsgTabItem.WEIBO.ordinal -> {
							coroutineScope.launch {
								Coroutines.io {
									model.msgModel.weiboState.requestWeibo()
								}
							}
						}
						MsgTabItem.CHAOHUA.ordinal -> {

						}
						MsgTabItem.PICTURES.ordinal -> {

						}
					}
				}
			)
			Segment(
				modifier = Modifier.weight(1f),
				items = MsgTabItem.entries.map { it.title },
				currentIndex = pagerState.currentPage,
				onSelected = {
					coroutineScope.launch {
						pagerState.animateScrollToPage(it)
					}
				}
			)
			ClickIcon(
				imageVector = Icons.Filled.AccountCircle,
				onClick = {
					model.msgModel.weiboState.state = when (model.msgModel.weiboState.state) {
						BoxState.CONTENT -> BoxState.LOADING
						BoxState.LOADING -> BoxState.EMPTY
						BoxState.EMPTY -> BoxState.NETWORK_ERROR
						BoxState.NETWORK_ERROR -> BoxState.CONTENT
					}
				}
			)
		}
		HorizontalPager(
			state = pagerState,
			modifier = Modifier.fillMaxWidth().weight(1f),
			userScrollEnabled = false
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				when (it) {
					MsgTabItem.WEIBO.ordinal -> ScreenWeibo(model)
					MsgTabItem.CHAOHUA.ordinal -> ScreenChaohua(model)
					MsgTabItem.PICTURES.ordinal -> ScreenPictures(model)
				}
			}
		}
	}
}