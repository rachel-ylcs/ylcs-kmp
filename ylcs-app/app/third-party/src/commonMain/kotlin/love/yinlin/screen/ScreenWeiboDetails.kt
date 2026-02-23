package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.common.DataSourceWeibo
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.common.WeiboLayout
import love.yinlin.compose.ui.common.WeiboUserBar
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenWeiboDetails : Screen() {
    private val currentWeibo: Weibo? = DataSourceWeibo.currentWeibo
    private var comments: List<WeiboComment>? by mutableRefStateOf(null)

    @Composable
    private fun WeiboCommentCard(modifier: Modifier, comment: WeiboComment) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
        ) {
            WeiboUserBar(
                info = comment.info,
                time = comment.timeString,
                location = comment.location
            )
            RachelRichText(
                text = comment.text,
                modifier = Modifier.fillMaxWidth(),
                onLinkClick = {
                    with(DataSourceWeibo.processor) { onWeiboLinkClick(it) }
                },
                onTopicClick = {
                    with(DataSourceWeibo.processor) { onWeiboTopicClick(it) }
                },
                onAtClick = {
                    with(DataSourceWeibo.processor) { onWeiboAtClick(it) }
                }
            )
            val subComments = comment.subComments
            if (subComments.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(start = Theme.size.image9),
                    tonalLevel = 5,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        for (subComment in subComments) {
                            WeiboUserBar(
                                info = subComment.info,
                                location = subComment.location,
                                time = subComment.timeString
                            )
                            RachelRichText(
                                text = subComment.text,
                                modifier = Modifier.fillMaxWidth(),
                                onLinkClick = {
                                    with(DataSourceWeibo.processor) { onWeiboLinkClick(it) }
                                },
                                onTopicClick = {
                                    with(DataSourceWeibo.processor) { onWeiboTopicClick(it) }
                                },
                                onAtClick = {
                                    with(DataSourceWeibo.processor) { onWeiboAtClick(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(weibo: Weibo) {
        LazyColumn(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            item(key = ItemKey("WeiboLayout")) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                    contentPadding = Theme.padding.eValue,
                    shadowElevation = Theme.shadow.v3,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        WeiboLayout(weibo = weibo, downloadDialog = null)
                    }
                }
            }
            comments?.let { weiboComments ->
                items(
                    items = weiboComments,
                    key = { it.id }
                ) {
                    WeiboCommentCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e), comment = it)
                }
            }
        }
    }

    @Composable
    private fun Landscape(weibo: Weibo) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Surface(
                modifier = Modifier.width(Theme.size.cell1).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
                contentPadding = Theme.padding.eValue,
                shadowElevation = Theme.shadow.v3,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                ) {
                    WeiboLayout(weibo = weibo, downloadDialog = null)
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(Theme.padding.e),
                contentAlignment = Alignment.Center
            ) {
                val weiboComments = comments
                if (weiboComments == null) CircleLoading.Content()
                else if (weiboComments.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        items(
                            items = weiboComments,
                            key = { it.id }
                        ) {
                            WeiboCommentCard(modifier = Modifier.fillMaxWidth(), comment = it)
                        }
                    }
                }
            }
        }
    }

    override val title: String = "微博详情"

    override suspend fun initialize() {
        currentWeibo?.let { comments = WeiboAPI.getWeiboDetails(it.id) ?: emptyList() }
    }

    @Composable
    override fun Content() {
        currentWeibo?.let {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait(weibo = it)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(weibo = it)
            }
        }
    }
}