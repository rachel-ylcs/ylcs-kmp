package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.common.BasicWeiboManager
import love.yinlin.common.MessageType
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.itemKey
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.extension.then
import love.yinlin.tpl.weibo.WeiboAPI

@Stable
class ScreenWeiboDetails(private val weibo: Weibo) : Screen() {
    private var comments: List<WeiboComment>? by mutableRefStateOf(null)

    init {
        land(BasicWeiboManager.CommonDownloadDialog)
    }

    @Composable
    private fun WeiboCommentCard(manager: BasicWeiboManager, modifier: Modifier, comment: WeiboComment) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
        ) {
            with(manager) {
                MessageUserLayout(modifier = Modifier.fillMaxWidth(), message = comment)
                MessageTextRender(modifier = Modifier.fillMaxWidth(), text = comment.content, style = Theme.typography.v7)

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
                                key(subComment.id) {
                                    MessageUserLayout(modifier = Modifier.fillMaxWidth(), message = subComment)
                                    MessageTextRender(modifier = Modifier.fillMaxWidth(), text = subComment.content, style = Theme.typography.v7)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val weiboInfoLayout = movableComposable { manager: BasicWeiboManager, outerModifier: Modifier, innerModifier: Modifier ->
        Surface(
            modifier = outerModifier,
            contentAlignment = Alignment.TopCenter,
            contentPadding = Theme.padding.eValue,
            shadowElevation = Theme.shadow.v3,
        ) {
            Column(
                modifier = innerModifier,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                with(manager) {
                    MessageLayout(
                        modifier = Modifier.fillMaxWidth(),
                        message = weibo
                    )
                }
            }
        }
    }

    @Composable
    private fun Portrait(manager: BasicWeiboManager) {
        LazyColumn(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            itemKey("WeiboLayout") {
                weiboInfoLayout(manager, Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
            }
            comments?.then { weiboComments ->
                items(items = weiboComments, key = { it.id }) {
                    WeiboCommentCard(manager = manager, modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e), comment = it)
                }
            }
        }
    }

    @Composable
    private fun Landscape(manager: BasicWeiboManager) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            weiboInfoLayout(manager, Modifier.width(Theme.size.cell1).fillMaxHeight(), Modifier.fillMaxSize().verticalScroll(rememberScrollState()))
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
                        items(items = weiboComments, key = { it.id }) {
                            WeiboCommentCard(manager = manager, modifier = Modifier.fillMaxWidth(), comment = it)
                        }
                    }
                }
            }
        }
    }

    override val title: String = "微博详情"

    override suspend fun initialize() {
        val cookie = DataSourceInformation.fetchWeiboCookie()
        comments = WeiboAPI.requestWeiboComment(weibo.id, cookie) ?: emptyList()
    }

    @Composable
    override fun Content() {
        val deviceType by rememberDeviceType()
        val manager = DataSourceInformation.managers[MessageType.Weibo]!!
        when (deviceType) {
            Device.Type.PORTRAIT -> Portrait(manager = manager)
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(manager = manager)
        }
    }
}