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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.api.WeiboAPI
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.String
import love.yinlin.extension.launchFlag
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.container.TabBar
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenVideo
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

@Stable
@Serializable
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
sealed class PhotoItem(val name: String) {
	override fun toString(): String = name

	@Stable
	class File(name: String) : PhotoItem(name) {
		val thumb: String = "https://img.picgo.net/$name.md.webp"
		val source: String = "https://img.picgo.net/$name.webp"
	}

	@Stable
	class Folder (name: String, val items: List<PhotoItem>) : PhotoItem(name)

	companion object {
		val Home: Folder = Folder("相册", emptyList())

		fun parseJson(name: String, json: JsonObject): Folder {
			val items = mutableListOf<PhotoItem>()
			val folder = Folder(name, items)
			for ((key, value) in json) {
				items += when (value) {
					is JsonObject -> parseJson(key, value)
					is JsonArray -> Folder(key, value.map { File(it.String) })
					else -> error("")
				}
			}
			return folder
		}
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

	private val pagerState = object : PagerState() {
		override val pageCount: Int = MsgTabItem.entries.size
	}
	// 当前微博
	var currentWeibo: Weibo? = null

	val processor = object : WeiboProcessor {
		override fun onWeiboClick(weibo: Weibo) {
			currentWeibo = weibo
			navigate<ScreenWeiboDetails>()
		}

		override fun onWeiboAvatarClick(info: WeiboUserInfo) {
			navigate(ScreenWeiboUser.Args(info.id))
		}

		private fun gotoWebPage(arg: String) {
			OS.ifPlatform(
				Platform.WebWasm, *Platform.Desktop,
				ifTrue = {
					OS.Net.openUrl(arg)
				},
				ifFalse = {
					navigate(ScreenWebpage.Args(arg))
				}
			)
		}

		override fun onWeiboLinkClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboTopicClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboAtClick(arg: String) = gotoWebPage(arg)

		override fun onWeiboPicClick(pics: List<Picture>, current: Int) {
			navigate(ScreenImagePreview.Args(pics, current))
		}

		override fun onWeiboVideoClick(pic: Picture) {
			navigate(ScreenVideo.Args(pic.video))
		}
	}

	@Stable
	class PhotoState {
		val flagFirstLoad = launchFlag()
		var photos by mutableStateOf(PhotoItem.Home)
		var stack = mutableStateListOf(photos)
		var state by mutableStateOf(BoxState.EMPTY)

		suspend fun loadPhotos() {
			if (state != BoxState.LOADING) {
				state = BoxState.LOADING
				val result = ClientAPI.request<JsonObject>(route = ServerRes.Photo)
				val data = Coroutines.cpu {
					try {
						PhotoItem.parseJson("相册", (result as Data.Success).data)
					} catch (_: Throwable) {
						null
					}
				}
				if (data != null) {
					photos = data
					stack.clear()
					stack += photos
					state = BoxState.CONTENT
				}
				else state = BoxState.NETWORK_ERROR
			}
		}
	}

	// 微博数据
	val weiboState = WeiboState()
	// 超话数据
	val chaohuaState = ChaohuaState()
	// 图集数据
	val photoState = PhotoState()

	private suspend fun onPageChanged(index: Int) {
		pagerState.scrollToPage(index)
	}

	private suspend fun onRefresh() {
		when (pagerState.currentPage) {
			MsgTabItem.WEIBO.ordinal -> weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
			MsgTabItem.CHAOHUA.ordinal -> chaohuaState.requestNewData()
			MsgTabItem.PICTURES.ordinal -> photoState.loadPhotos()
		}
	}

	override suspend fun initialize() {
		weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
	}

	@Composable
	override fun Content() {
		Column(modifier = Modifier.fillMaxSize()) {
			val immersivePadding = LocalImmersivePadding.current

			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(immersivePadding.withoutBottom),
					verticalAlignment = Alignment.CenterVertically
				) {
					TabBar(
						currentPage = pagerState.currentPage,
						onNavigate = {
							launch { onPageChanged(it) }
						},
						items = MsgTabItem.items,
						modifier = Modifier.weight(1f)
					)
					Space()
					ActionScope.Right.Actions {
						ActionSuspend(Icons.Outlined.Refresh) {
							onRefresh()
						}
						Action(Icons.Filled.AccountCircle) {
							navigate<ScreenWeiboFollows>()
						}
					}
				}
			}

			HorizontalPager(
				state = pagerState,
				key = { MsgTabItem.entries[it] },
				beyondViewportPageCount = MsgTabItem.entries.size,
				modifier = Modifier.fillMaxWidth().weight(1f).padding(immersivePadding.withoutTop),
				userScrollEnabled = false
			) {
				Box(modifier = Modifier.fillMaxSize()) {
					CompositionLocalProvider(LocalWeiboProcessor provides processor) {
						when (it) {
							MsgTabItem.WEIBO.ordinal -> ScreenWeibo(this@ScreenPartMsg)
							MsgTabItem.CHAOHUA.ordinal -> ScreenChaohua(this@ScreenPartMsg)
							MsgTabItem.PICTURES.ordinal -> ScreenPictures(this@ScreenPartMsg)
						}
					}
				}
			}

			LaunchedEffect(pagerState.settledPage) {
				when (pagerState.currentPage) {
					MsgTabItem.WEIBO.ordinal -> weiboState.flagFirstLoad.update(this) {

					}
					MsgTabItem.CHAOHUA.ordinal -> chaohuaState.flagFirstLoad.update(this) {
						chaohuaState.requestNewData()
					}
					MsgTabItem.PICTURES.ordinal -> photoState.flagFirstLoad.update(this) {
						photoState.loadPhotos()
					}
				}
			}
		}
	}
}