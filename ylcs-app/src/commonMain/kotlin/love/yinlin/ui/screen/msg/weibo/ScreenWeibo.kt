package love.yinlin.ui.screen.msg.weibo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.filenameOrRandom
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import love.yinlin.platform.Picker
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText
import love.yinlin.platform.app
import love.yinlin.platform.safeDownload
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.dialog.FloatingDownloadDialog

@Stable
class ScreenWeibo(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)
    private var items = mutableStateListOf<Weibo>()
    private val gridState = LazyStaggeredGridState()

    private suspend fun requestWeibo() {
        if (state != BoxState.LOADING) {
            val users = app.config.weiboUsers.map { it.id }
            if (users.isEmpty()) state = BoxState.EMPTY
            else {
                state = BoxState.LOADING
                items.clear()
                for (id in users) {
                    val result = WeiboAPI.getUserWeibo(id)
                    if (result is Data.Success) {
                        items += result.data
                        items.sortDescending()
                        if (state == BoxState.LOADING) state = BoxState.CONTENT
                        gridState.scrollToItem(0)
                    }
                }
                if (state == BoxState.LOADING) state = BoxState.NETWORK_ERROR
            }
        }
    }

    override val title: String = "微博"

    @Composable
    override fun ActionScope.RightActions() {
        Action(Icons.Outlined.AccountCircle, "关注列表") {
            navigate<ScreenWeiboFollows>()
        }
    }

    override suspend fun initialize() {
        requestWeibo()
    }

    @Composable
    override fun SubContent(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
            StatefulBox(
                state = state,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                WeiboGrid(
                    state = gridState,
                    items = items,
                    modifier = Modifier.fillMaxSize(),
                    onPicturesDownload = { pics ->
                        OS.ifPlatform(
                            *Platform.Phone,
                            ifTrue = {
                                launch {
                                    slot.loading.openSuspend()
                                    Coroutines.io {
                                        for (pic in pics) {
                                            val url = pic.source
                                            val filename = url.filenameOrRandom(".webp")
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
                    },
                    onVideoDownload = { url ->
                        val filename = url.filenameOrRandom(".mp4")
                        launch {
                            Coroutines.io {
                                Picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
                                    val result = downloadVideoDialog.openSuspend(url, sink) { Picker.actualSave(filename, origin, sink) }
                                    Picker.cleanSave(origin, result)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestWeibo() }
        else gridState.animateScrollToItem(0)
    }

    private val downloadVideoDialog = FloatingDownloadDialog()

    @Composable
    override fun Floating() {
        downloadVideoDialog.Land()
    }
}