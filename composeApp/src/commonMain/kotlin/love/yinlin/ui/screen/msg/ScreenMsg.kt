package love.yinlin.ui.screen.msg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.data.item.MsgTabItem
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.LaunchFlag
import love.yinlin.ui.Route
import love.yinlin.ui.common.WeiboGridData
import love.yinlin.ui.component.ClickIcon
import love.yinlin.ui.component.Segment
import love.yinlin.ui.screen.MainModel
import love.yinlin.ui.screen.msg.weibo.ScreenChaohua
import love.yinlin.ui.screen.msg.weibo.ScreenWeibo

class MsgModel(val mainModel: MainModel) {
	class WeiboState {
		val launchFlag = LaunchFlag()
		val grid = WeiboGridData()
	}

	class ChaohuaState {

	}

	val pagerState = object : PagerState() {
		override val pageCount: Int = MsgTabItem.entries.size
	}
	// 当前微博
	var currentWeibo: Weibo? = null
	// 关注用户
	val followUsers = app.config.weiboUsers.map {
		WeiboUserInfo(it.id, it.name, "")
	}.toMutableStateList()
	// 微博数据
	val weiboState = WeiboState()
	// 超话数据
	val chaohuaState = ChaohuaState()

	fun onNavigate(index: Int) {
		mainModel.launch { pagerState.scrollToPage(index) }
	}

	fun onRefresh() {
		when (pagerState.currentPage) {
			MsgTabItem.WEIBO.ordinal -> mainModel.launch {
				weiboState.grid.requestWeibo(followUsers.map { it.id })
			}
			MsgTabItem.CHAOHUA.ordinal -> {

			}
			MsgTabItem.PICTURES.ordinal -> {

			}
		}
	}

	fun onWeiboClick(weibo: Weibo) {
		currentWeibo = weibo
		mainModel.navigate(Route.WeiboDetails)
	}

	fun onWeiboAvatarClick(info: WeiboUserInfo) {
		mainModel.navigate(Route.WeiboUser(info.id))
	}

	fun onWeiboLinkClick(arg: String) {

	}

	fun onWeiboTopicClick(arg: String) {

	}

	fun onWeiboAtClick(arg: String) {

	}
}

@Composable
fun ScreenMsg(model: MsgModel) {
	Column(modifier = Modifier.fillMaxSize()) {
		Surface(
			modifier = Modifier.fillMaxWidth().zIndex(5f),
			shadowElevation = 5.dp
		) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				ClickIcon(
					imageVector = Icons.Filled.Refresh,
					onClick = { model.onRefresh() }
				)
				Segment(
					modifier = Modifier.weight(1f),
					items = MsgTabItem.entries.map { it.title },
					currentIndex = model.pagerState.currentPage,
					onSelected = { model.onNavigate(it) }
				)
				ClickIcon(
					imageVector = Icons.Filled.AccountCircle,
					onClick = { model.mainModel.navigate(Route.WeiboFollows) }
				)
			}
		}
		HorizontalPager(
			state = model.pagerState,
			modifier = Modifier.fillMaxWidth().weight(1f),
			userScrollEnabled = false
		) {
			Box(modifier = Modifier.fillMaxSize()) {
				when (it) {
					MsgTabItem.WEIBO.ordinal -> ScreenWeibo(model)
					MsgTabItem.CHAOHUA.ordinal -> ScreenChaohua()
					MsgTabItem.PICTURES.ordinal -> ScreenPictures()
				}
			}
		}
	}
}