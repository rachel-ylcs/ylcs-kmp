package love.yinlin.ui.screen.msg.weibo

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
import com.github.panpf.sketch.ability.bindPauseLoadWhenScrolling
import love.yinlin.AppModel
import love.yinlin.api.WeiboAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.ItemKey
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.text.RichText

@Stable
class ScreenWeiboDetails(model: AppModel) : CommonSubScreen(model) {
    private val weibo: Weibo? = msgPart.currentWeibo
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
                        top = ThemeValue.Padding.VerticalSpace,
                        start = ThemeValue.Padding.HorizontalExtraSpace * 1.5f
                    ),
                    tonalElevation = ThemeValue.Shadow.Tonal
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value)) {
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
        bindPauseLoadWhenScrolling(listState)

        LazyColumn(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(horizontal = ThemeValue.Padding.EqualExtraSpace),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
        ) {
            item(key = ItemKey("WeiboLayout")) {
                Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
                WeiboLayout(
                    weibo = weibo,
                    onPicturesDownload = null,
                    onVideoDownload = null
                )
            }
            comments?.let { weiboComments ->
                item(key = ItemKey("HorizontalDivider")) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = ThemeValue.Padding.VerticalExtraSpace))
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
                .padding(horizontal = ThemeValue.Padding.EqualExtraSpace)
        ) {
            Column(modifier = Modifier.width(ThemeValue.Size.PanelWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalExtraSpace))
                WeiboLayout(
                    weibo = weibo,
                    onPicturesDownload = null,
                    onVideoDownload = null
                )
            }
            VerticalDivider(modifier = Modifier.padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace))
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val weiboComments = comments
                if (weiboComments == null) LoadingBox()
                else if (weiboComments.isEmpty()) EmptyBox()
                else {
                    val listState = rememberLazyListState()
                    bindPauseLoadWhenScrolling(listState)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
                    ) {
                        item(key = ItemKey("Space")) {
                            Spacer(modifier = Modifier.height(ThemeValue.Padding.VerticalSpace))
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
            val data = WeiboAPI.getWeiboDetails(it.id)
            comments = if (data is Data.Success) data.data else emptyList()
        }
    }

    override val title: String = "微博详情"

    @Composable
    override fun SubContent(device: Device) {
        CompositionLocalProvider(LocalWeiboProcessor provides msgPart.processor) {
            weibo?.let {
                when (device.type) {
                    Device.Type.PORTRAIT -> Portrait(weibo = it)
                    Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(weibo = it)
                }
            } ?: EmptyBox()
        }
    }
}