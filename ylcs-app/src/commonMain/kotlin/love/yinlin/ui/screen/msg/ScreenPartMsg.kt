package love.yinlin.ui.screen.msg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.launchFlag
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.component.screen.ActionScope
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenWebpage
import love.yinlin.ui.screen.msg.pictures.ScreenPictures
import love.yinlin.ui.screen.msg.weibo.LocalWeiboProcessor
import love.yinlin.ui.screen.msg.weibo.ScreenChaohua
import love.yinlin.ui.screen.msg.weibo.ScreenWeibo
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboDetails
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboFollows
import love.yinlin.ui.screen.msg.weibo.ScreenWeiboUser
import love.yinlin.ui.screen.msg.weibo.WeiboGridData
import love.yinlin.ui.screen.msg.weibo.WeiboProcessor

private enum class MsgTabItem(
	val title: String,
	val icon: ImageVector
) {
	WEIBO("微博", Icons.Filled.Newspaper),
	CHAOHUA("超话", Icons.Filled.Diversity1),
	PICTURES("美图", Icons.Filled.PhotoLibrary);

	companion object {
		@Stable
		val items = MsgTabItem.entries.map { it.title to it.icon }
	}
}

@Stable
class ScreenPartMsg(model: AppModel) : ScreenPart(model) {
	@Stable
	class WeiboState {
		val flagFirstLoad = launchFlag()
		val grid = WeiboGridData()
	}

	@Stable
	class ChaohuaState {
		val flagFirstLoad = launchFlag()
		val grid = WeiboGridData()
		var sinceId: Long = 0L
		var canLoading by mutableStateOf(false)

		suspend fun requestNewData() {
			if (grid.state != BoxState.LOADING) {
				grid.state = BoxState.LOADING
				canLoading = false
				val result = WeiboAPI.extractChaohua(0L)
				grid.state = if (result is Data.Success) {
					val (data, newSinceId) = result.data
					sinceId = newSinceId
					canLoading = newSinceId != 0L
					grid.items = data
					if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
				}
				else BoxState.NETWORK_ERROR
			}
		}

		suspend fun requestMoreData() {
			val result = WeiboAPI.extractChaohua(sinceId)
			if (result is Data.Success) {
				val (data, newSinceId) = result.data
				sinceId = newSinceId
				canLoading = newSinceId != 0L
				grid.items += data
			}
		}
	}

	val pagerState = object : PagerState() {
		override val pageCount: Int = MsgTabItem.entries.size
	}
	// 当前微博
	var currentWeibo: Weibo? = null

	// 微博数据
	val weiboState = WeiboState()
	// 超话数据
	val chaohuaState = ChaohuaState()

	fun onPageChanged(index: Int) {
		launch { pagerState.scrollToPage(index) }
	}

	fun onRefresh() {
		when (pagerState.currentPage) {
			MsgTabItem.WEIBO.ordinal -> launch {
				weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
			}
			MsgTabItem.CHAOHUA.ordinal -> launch {
				chaohuaState.requestNewData()
			}
			MsgTabItem.PICTURES.ordinal -> {

			}
		}
	}

	val processor = object : WeiboProcessor {
		override fun onWeiboClick(weibo: Weibo) {
			currentWeibo = weibo
			navigate(ScreenWeiboDetails.Args)
		}

		override fun onWeiboAvatarClick(info: WeiboUserInfo) {
			navigate(ScreenWeiboUser.Args(info.id))
		}

		override fun onWeiboLinkClick(arg: String) {
			if (OS.platform.isWeb) OS.Net.openUrl(arg)
			else navigate(ScreenWebpage.Args(arg))
		}

		override fun onWeiboTopicClick(arg: String) {
			if (OS.platform.isWeb) OS.Net.openUrl(arg)
			else navigate(ScreenWebpage.Args(arg))
		}

		override fun onWeiboAtClick(arg: String) {
			if (OS.platform.isWeb) OS.Net.openUrl(arg)
			else navigate(ScreenWebpage.Args(arg))
		}

		override fun onWeiboPicClick(pics: List<Picture>, current: Int) {
			navigate(ScreenImagePreview.Args(pics, current))
		}

		override fun onWeiboVideoClick(pic: Picture) {
			TODO()
		}
	}

	@Composable
	override fun content() {
		Column(modifier = Modifier.fillMaxSize()) {
			Surface(
				modifier = Modifier.fillMaxWidth().zIndex(5f),
				shadowElevation = 5.dp
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					TabBar(
						currentPage = pagerState.currentPage,
						onNavigate = { onPageChanged(it) },
						items = MsgTabItem.items,
						modifier = Modifier.weight(1f).padding(end = 10.dp)
					)
					ActionScope.Right.Actions {
						Action(Icons.Outlined.Refresh) {
							onRefresh()
						}
						Action(Icons.Filled.AccountCircle) {
							navigate(ScreenWeiboFollows.Args)
						}
					}
				}
			}
			HorizontalPager(
				state = pagerState,
				modifier = Modifier.fillMaxWidth().weight(1f),
				userScrollEnabled = false
			) {
				Box(modifier = Modifier.fillMaxSize()) {
					CompositionLocalProvider(LocalWeiboProcessor provides processor) {
						when (it) {
							MsgTabItem.WEIBO.ordinal -> ScreenWeibo(this@ScreenPartMsg)
							MsgTabItem.CHAOHUA.ordinal -> ScreenChaohua(this@ScreenPartMsg)
							MsgTabItem.PICTURES.ordinal -> ScreenPictures()
						}
					}
				}
			}
		}
	}
}