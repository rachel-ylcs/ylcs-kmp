package love.yinlin.screen.msg.weibo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.data.Data
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.filenameOrRandom
import love.yinlin.platform.*
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.compose.ui.layout.PaginationStaggeredGrid
import love.yinlin.compose.ui.floating.FloatingDownloadDialog

@Stable
class ScreenChaohua(manager: ScreenManager) : Screen(manager) {
    private val subScreenMsg = manager.get<ScreenMain>().get<SubScreenMsg>()

    private var state by mutableStateOf(BoxState.EMPTY)
    private var items by mutableRefStateOf(emptyList<Weibo>())
    private val gridState = LazyStaggeredGridState()
    private var sinceId: Long = 0L
    private var canLoading by mutableStateOf(false)

    private suspend fun requestNewData(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            canLoading = false

            // 微博需要获取subCookie
            if (WeiboAPI.subCookie == null) WeiboAPI.subCookie = WeiboAPI.generateWeiboSubCookie()

            val result = WeiboAPI.extractChaohua(0L)
            state = if (result is Data.Success) {
                val (data, newSinceId) = result.data
                sinceId = newSinceId
                canLoading = newSinceId != 0L
                items = data
                if (data.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
            }
            else {
                WeiboAPI.subCookie = null
                BoxState.NETWORK_ERROR
            }
        }
    }

    private suspend fun requestMoreData() {
        val result = WeiboAPI.extractChaohua(sinceId)
        if (result is Data.Success) {
            val (data, newSinceId) = result.data
            sinceId = newSinceId
            canLoading = newSinceId != 0L
            items += data
        }
        else WeiboAPI.subCookie = null
    }

    override val title: String = "超话"

    override suspend fun initialize() {
        requestNewData(true)
    }

    @Composable
    override fun Content(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides subScreenMsg.processor) {
            StatefulBox(
                state = state,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                PauseLoading(gridState)

                PaginationStaggeredGrid(
                    items = items,
                    key = { it.id },
                    columns = StaggeredGridCells.Adaptive(CustomTheme.size.cardWidth),
                    state = gridState,
                    canRefresh = true,
                    canLoading = canLoading,
                    onRefresh = { requestNewData(false) },
                    onLoading = { requestMoreData() },
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = CustomTheme.padding.equalValue,
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                    verticalItemSpacing = CustomTheme.padding.equalSpace
                ) { weibo ->
                    WeiboCard(
                        weibo = weibo,
                        modifier = Modifier.fillMaxWidth(),
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
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector by derivedStateOf { if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewData(true) }
        else gridState.animateScrollToItem(0)
    }

    private val downloadVideoDialog = this land FloatingDownloadDialog()
}