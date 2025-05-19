package love.yinlin.ui.screen.msg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
import love.yinlin.platform.*
import love.yinlin.ui.component.container.TabBar
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.screen.FABAction
import love.yinlin.ui.component.screen.dialog.FloatingDownloadDialog
import love.yinlin.ui.screen.common.ScreenImagePreview
import love.yinlin.ui.screen.common.ScreenVideo
import love.yinlin.ui.screen.common.ScreenWebpage
import love.yinlin.ui.screen.msg.pictures.PagePictures
import love.yinlin.ui.screen.msg.weibo.*

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
		val firstLoad = launchFlag()
		val grid = WeiboGridData()
	}

	@Stable
	class ChaohuaState {
		val firstLoad = launchFlag()
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

	private var currentPage by mutableIntStateOf(0)

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

		override fun onWeiboPicsDownload(pics: List<Picture>) {
			OS.ifPlatform(
				*Platform.Phone,
				ifTrue = {
					launch {
						slot.loading.openSuspend()
						Coroutines.io {
							for (pic in pics) {
								val url = pic.source
								val filename = url.substringAfterLast('/').substringBefore('?')
								Picker.prepareSavePicture(filename)?.let { (origin, sink) ->
									val result = sink.use {
										val result = app.fileClient.safeDownload(
											url = url,
											sink = it,
											isCancel = { false },
											onGetSize = {},
											onTick = { _, _ -> }
										)
										if (result) Picker.actualSave(filename, origin, sink)
										result
									}
									Picker.cleanSave(origin, result)
								}
							}
						}
						slot.loading.close()
					}
				},
				ifFalse = {
					slot.tip.warning(UnsupportedPlatformText)
				}
			)
		}

		override fun onWeiboVideoClick(pic: Picture) {
			navigate(ScreenVideo.Args(pic.video))
		}

		override fun onWeiboVideoDownload(url: String) {
			val filename = url.substringAfterLast('/').substringBefore('?')
			launch {
				Coroutines.io {
					Picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
						val result = downloadVideoDialog.openSuspend(url, sink) { Picker.actualSave(filename, origin, sink) }
						Picker.cleanSave(origin, result)
					}
				}
			}
		}
	}

	@Stable
	class PhotoState {
		val firstLoad = launchFlag()
		var photos by mutableStateOf(PhotoItem.Home)
		var stack = mutableStateListOf(photos)
		var state by mutableStateOf(BoxState.EMPTY)
		val listState = LazyGridState()

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

	val weiboState = WeiboState()
	val chaohuaState = ChaohuaState()
	val photoState = PhotoState()

	private suspend fun initializeNewData() {
		when (currentPage) {
			MsgTabItem.WEIBO.ordinal -> weiboState.firstLoad { weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id }) }
			MsgTabItem.CHAOHUA.ordinal -> chaohuaState.firstLoad { chaohuaState.requestNewData() }
			MsgTabItem.PICTURES.ordinal -> photoState.firstLoad { photoState.loadPhotos() }
		}
	}

	override suspend fun initialize() {
		initializeNewData()
	}

	@Composable
	override fun Content() {
		Column(modifier = Modifier.fillMaxSize()) {
			val immersivePadding = LocalImmersivePadding.current

			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = ThemeValue.Shadow.Surface
			) {
				TabBar(
					currentPage = currentPage,
					onNavigate = {
						currentPage = it
						launch { initializeNewData() }
					},
					items = MsgTabItem.items,
					modifier = Modifier.fillMaxWidth().padding(immersivePadding.withoutBottom),
				)
			}

			Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(immersivePadding.withoutTop)) {
				CompositionLocalProvider(LocalWeiboProcessor provides processor) {
					when (currentPage) {
						MsgTabItem.WEIBO.ordinal -> PageWeibo(
							part = this@ScreenPartMsg,
							state = weiboState.grid.listState
						)
						MsgTabItem.CHAOHUA.ordinal -> PageChaohua(
							part = this@ScreenPartMsg,
							state = chaohuaState.grid.listState
						)
						MsgTabItem.PICTURES.ordinal -> PagePictures(
							part = this@ScreenPartMsg,
							state = photoState.listState
						)
					}
				}
			}
		}
	}

	override val fabCanExpand: Boolean by derivedStateOf { when (currentPage) {
		MsgTabItem.WEIBO.ordinal -> weiboState.grid.listState.firstVisibleItemIndex == 0 && weiboState.grid.listState.firstVisibleItemScrollOffset == 0
		MsgTabItem.CHAOHUA.ordinal -> chaohuaState.grid.listState.firstVisibleItemIndex == 0 && chaohuaState.grid.listState.firstVisibleItemScrollOffset == 0
		MsgTabItem.PICTURES.ordinal -> photoState.listState.firstVisibleItemIndex == 0 && photoState.listState.firstVisibleItemScrollOffset == 0
		else -> true
	} }

	override val fabIcon: ImageVector? by derivedStateOf { if (fabCanExpand) Icons.Outlined.Add else Icons.Outlined.ArrowUpward }

	override val fabMenus: Array<FABAction> = arrayOf(
		FABAction(Icons.Filled.AccountCircle) {
			navigate<ScreenWeiboFollows>()
		},
		FABAction(Icons.Outlined.Refresh) {
			launch {
				when (currentPage) {
					MsgTabItem.WEIBO.ordinal -> weiboState.grid.requestWeibo(app.config.weiboUsers.map { it.id })
					MsgTabItem.CHAOHUA.ordinal -> chaohuaState.requestNewData()
					MsgTabItem.PICTURES.ordinal -> photoState.loadPhotos()
				}
			}
		}
	)

	override suspend fun onFabClick() {
		when (currentPage) {
			MsgTabItem.WEIBO.ordinal -> weiboState.grid.listState.animateScrollToItem(0)
			MsgTabItem.CHAOHUA.ordinal -> chaohuaState.grid.listState.animateScrollToItem(0)
			MsgTabItem.PICTURES.ordinal -> photoState.listState.animateScrollToItem(0)
		}
	}

	private val downloadVideoDialog = FloatingDownloadDialog()

	@Composable
	override fun Floating() {
		downloadVideoDialog.Land()
	}
}