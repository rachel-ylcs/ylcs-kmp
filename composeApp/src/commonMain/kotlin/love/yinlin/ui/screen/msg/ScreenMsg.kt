package love.yinlin.ui.screen.msg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import love.yinlin.api.WeiboAPI
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.launchFlag
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.Route
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.screen.MainModel
import love.yinlin.ui.screen.msg.pictures.ScreenPictures
import love.yinlin.ui.screen.msg.weibo.ScreenChaohua
import love.yinlin.ui.screen.msg.weibo.ScreenWeibo
import love.yinlin.ui.screen.msg.weibo.WeiboGridData
import love.yinlin.ui.screen.msg.weibo.WeiboLayout

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

class MsgModel(val mainModel: MainModel) {
	class WeiboState {
		val flagFirstLoad = launchFlag()
		val grid = WeiboGridData()
	}

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

	fun onNavigate(index: Int) {
		mainModel.launch { pagerState.scrollToPage(index) }
	}

	fun onRefresh() {
		when (pagerState.currentPage) {
			MsgTabItem.WEIBO.ordinal -> mainModel.launch {
				weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
			}
			MsgTabItem.CHAOHUA.ordinal -> mainModel.launch {
				chaohuaState.requestNewData()
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
		if (OS.platform.isWeb) OS.openURL(arg)
		else mainModel.navigate(Route.WebPage(arg))
	}

	fun onWeiboTopicClick(arg: String) {
		if (OS.platform.isWeb) OS.openURL(arg)
		else mainModel.navigate(Route.WebPage(arg))
	}

	fun onWeiboAtClick(arg: String) {
		if (OS.platform.isWeb) OS.openURL(arg)
		else mainModel.navigate(Route.WebPage(arg))
	}

	fun onWeiboPicClick(pics: List<Picture>, current: Int) {
		mainModel.navigate(Route.ImagePreview(pics, current))
	}

	fun onWeiboVideoClick(pic: Picture) {
		TODO()
	}

	@Composable
	fun WeiboCard(
		weibo: Weibo,
		modifier: Modifier = Modifier
	) {
		ElevatedCard(
			modifier = modifier,
			colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surface),
			onClick = { onWeiboClick(weibo) }
		) {
			Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
				WeiboLayout(
					weibo = weibo,
					onAvatarClick = ::onWeiboAvatarClick,
					onLinkClick = ::onWeiboLinkClick,
					onTopicClick = ::onWeiboTopicClick,
					onAtClick = ::onWeiboAtClick,
					onImageClick = ::onWeiboPicClick,
					onVideoClick = ::onWeiboVideoClick
				)
			}
		}
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
				modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				TabBar(
					currentPage = model.pagerState.currentPage,
					onNavigate = { model.onNavigate(it) },
					items = MsgTabItem.items,
					modifier = Modifier.weight(1f)
				)
				ClickIcon(
					imageVector = Icons.Filled.Refresh,
					onClick = { model.onRefresh() }
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
					MsgTabItem.CHAOHUA.ordinal -> ScreenChaohua(model)
					MsgTabItem.PICTURES.ordinal -> ScreenPictures()
				}
			}
		}
	}
}