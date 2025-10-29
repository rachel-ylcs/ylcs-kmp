package love.yinlin.screen.msg.weibo

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
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.filenameOrRandom
import love.yinlin.platform.*
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.compose.ui.floating.FloatingDownloadDialog

@Stable
class ScreenWeibo(manager: ScreenManager) : CommonScreen(manager) {
    private val subScreenMsg = manager.get<ScreenMain>().get<SubScreenMsg>()

    private var state: BoxState by mutableStateOf(BoxState.EMPTY)
    private var items = mutableStateListOf<Weibo>()
    private val gridState = LazyStaggeredGridState()

    private suspend fun requestWeibo() {
        if (state != BoxState.LOADING) {
            val users = app.config.weiboUsers.map { it.id }
            if (users.isEmpty()) state = BoxState.EMPTY
            else {
                state = BoxState.LOADING

                // 微博需要获取subCookie
                if (WeiboAPI.subCookie == null) WeiboAPI.subCookie = WeiboAPI.generateWeiboSubCookie()

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
                if (state == BoxState.LOADING) {
                    WeiboAPI.subCookie = null
                    state = BoxState.NETWORK_ERROR
                }
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
    override fun Content(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides subScreenMsg.processor) {
            StatefulBox(
                state = state,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                WeiboGrid(
                    state = gridState,
                    items = items,
                    modifier = Modifier.fillMaxSize(),
                    onPicturesDownload = { pics ->
                        Platform.use(
                            *Platform.Phone,
                            ifTrue = {
                                launch {
                                    slot.loading.openSuspend()
                                    Coroutines.io {
                                        for (pic in pics) {
                                            val url = pic.source
                                            val filename = url.filenameOrRandom(".webp")
                                            val picker = app.picker
                                            picker.prepareSavePicture(filename)?.let { (origin, sink) ->
                                                val result = sink.use {
                                                    val result = NetClient.file.safeDownload(
                                                        url = url,
                                                        sink = it,
                                                        isCancel = { false },
                                                        onGetSize = {},
                                                        onTick = { _, _ -> }
                                                    )
                                                    if (result) picker.actualSave(filename, origin, sink)
                                                    result
                                                }
                                                picker.cleanSave(origin, result)
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
                                val picker = app.picker
                                picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
                                    val result = downloadVideoDialog.openSuspend(url, sink) { picker.actualSave(filename, origin, sink) }
                                    picker.cleanSave(origin, result)
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