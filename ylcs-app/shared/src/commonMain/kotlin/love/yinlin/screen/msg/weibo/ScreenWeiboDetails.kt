package love.yinlin.screen.msg.weibo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.api.WeiboAPI
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.LoadingBox
import love.yinlin.compose.ui.layout.Space
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.compose.ui.text.RichText

@Stable
class ScreenWeiboDetails(manager: ScreenManager) : Screen(manager) {
    private val subScreenMsg = manager.get<ScreenMain>().get<SubScreenMsg>()
    private val weibo: Weibo? = subScreenMsg.currentWeibo
    private var comments: List<WeiboComment>? by mutableRefStateOf(null)

    @Composable
    private fun WeiboCommentCard(comment: WeiboComment) {
        val processor = LocalWeiboProcessor.current
        Column(modifier = Modifier.fillMaxWidth()) {
            WeiboUserBar(
                info = comment.info,
                time = comment.timeString,
                location = comment.location
            )
            Space()
            RichText(
                text = comment.text,
                modifier = Modifier.fillMaxWidth(),
                onLinkClick = { processor.onWeiboLinkClick(it) },
                onTopicClick = { processor.onWeiboTopicClick(it) },
                onAtClick = { processor.onWeiboAtClick(it) }
            )
            Space()
            val subComments = comment.subComments
            if (subComments.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(
                        top = CustomTheme.padding.verticalSpace,
                        start = CustomTheme.padding.horizontalExtraSpace * 1.5f
                    ),
                    tonalElevation = CustomTheme.shadow.tonal
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value)) {
                        for (subComment in subComments) {
                            WeiboUserBar(
                                info = subComment.info,
                                location = subComment.location,
                                time = subComment.timeString
                            )
                            Space()
                            RichText(
                                text = subComment.text,
                                modifier = Modifier.fillMaxWidth(),
                                onLinkClick = { processor.onWeiboLinkClick(it) },
                                onTopicClick = { processor.onWeiboTopicClick(it) },
                                onAtClick = { processor.onWeiboAtClick(it) }
                            )
                            Space()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(weibo: Weibo) {
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(horizontal = CustomTheme.padding.equalExtraSpace),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            item(key = ItemKey("WeiboLayout")) {
                Spacer(modifier = Modifier.height(CustomTheme.padding.verticalExtraSpace))
                WeiboLayout(
                    weibo = weibo,
                    onPicturesDownload = null,
                    onVideoDownload = null
                )
            }
            comments?.let { weiboComments ->
                item(key = ItemKey("HorizontalDivider")) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = CustomTheme.padding.verticalExtraSpace))
                }
                items(
                    items = weiboComments,
                    key = { it.id }
                ) {
                    WeiboCommentCard(comment = it)
                }
            }
        }
    }

    @Composable
    private fun Landscape(weibo: Weibo) {
        Row(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(horizontal = CustomTheme.padding.equalExtraSpace)
        ) {
            Column(modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(CustomTheme.padding.verticalExtraSpace))
                WeiboLayout(
                    weibo = weibo,
                    onPicturesDownload = null,
                    onVideoDownload = null
                )
            }
            VerticalDivider(modifier = Modifier.padding(horizontal = CustomTheme.padding.horizontalExtraSpace))
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val weiboComments = comments
                if (weiboComments == null) LoadingBox()
                else if (weiboComments.isEmpty()) EmptyBox()
                else {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
                    ) {
                        item(key = ItemKey("Space")) {
                            Spacer(modifier = Modifier.height(CustomTheme.padding.verticalSpace))
                        }
                        items(
                            items = weiboComments,
                            key = { it.id }
                        ) {
                            WeiboCommentCard(comment = it)
                        }
                    }
                }
            }
        }
    }

    override suspend fun initialize() {
        weibo?.let {
            comments = WeiboAPI.getWeiboDetails(it.id) ?: emptyList()
        }
    }

    override val title: String = "微博详情"

    @Composable
    override fun Content(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides subScreenMsg.processor) {
            weibo?.let {
                when (device.type) {
                    Device.Type.PORTRAIT -> Portrait(weibo = it)
                    Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(weibo = it)
                }
            } ?: EmptyBox()
        }
    }
}