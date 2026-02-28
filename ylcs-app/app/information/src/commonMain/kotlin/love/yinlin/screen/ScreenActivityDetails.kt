package love.yinlin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import love.yinlin.app
import love.yinlin.app.information.resources.*
import love.yinlin.common.DataSourceInformation
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.ApiActivityDeleteActivity
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.findModify
import love.yinlin.platform.Platform
import love.yinlin.uri.Uri
import love.yinlin.uri.UriGenerator
import org.jetbrains.compose.resources.painterResource

@Stable
class ScreenActivityDetails(private val aid: Int) : Screen() {
    private val activities = DataSourceInformation.activities

    private val targetActivity: Activity? by derivedStateOf { activities.find { it.aid == aid } }

    private suspend fun deleteActivity() {
        ApiActivityDeleteActivity.request(app.config.userToken, aid) {
            activities.findModify(predicate = { it.aid == aid }) { this -= it }
            pop()
        }.errorTip
    }

    override val title: String get() = targetActivity?.shortTitle ?: targetActivity?.title ?: "未知活动"

    @Composable
    private fun LinkIcon(
        text: String,
        painter: Painter,
        onClick: () -> Unit
    ) {
        val shape = Theme.shape.v5
        Row(
            modifier = Modifier
                .clip(shape)
                .border(Theme.border.v9, Theme.color.outline, shape)
                .clickable(onClick = onClick)
                .padding(Theme.padding.value),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painter, color = Colors.Unspecified, modifier = Modifier.size(Theme.size.icon))
            SimpleEllipsisText(text = text)
        }
    }

    @Composable
    private fun ActivityInfoLayout(activity: Activity, modifier: Modifier = Modifier) {
        val coverPath = activity.photo.coverPath?.url ?: activity.photo.posters.firstOrNull()?.let { activity.photo.posterPath(it) }?.url

        Column(modifier = modifier) {
            // 封面
            if (coverPath != null) {
                WebImage(
                    uri = coverPath,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                )
            }

            // 标题
            Text(
                text = activity.title ?: activity.shortTitle ?: "未知活动",
                style = Theme.typography.v6.bold,
                modifier = Modifier.padding(Theme.padding.value)
            )

            // 时间地点
            TextIconAdapter(modifier = Modifier.padding(Theme.padding.value)) { idIcon, idText ->
                Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "时间: ${activity.ts ?: "未知"} ${activity.tsInfo ?: ""}", modifier = Modifier.idText())
            }
            TextIconAdapter(modifier = Modifier.padding(Theme.padding.value)) { idIcon, idText ->
                Icon(icon = Icons.AddLocation, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "地点: ${activity.location ?: "未知"}", modifier = Modifier.idText())
            }

            // 票价
            if (activity.price.isNotEmpty()) {
                SimpleEllipsisText(
                    text = "票价",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Theme.padding.v).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (item in activity.price) {
                        Surface(
                            modifier = Modifier.padding(horizontal = Theme.padding.h / 2),
                            shape = Theme.shape.v5,
                            contentPadding = Theme.padding.eValue10,
                            shadowElevation = Theme.shadow.v3,
                            border = BorderStroke(Theme.border.v9, Theme.color.outline),
                            onClick = { }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                            ) {
                                Text(
                                    text = "￥${item.value}",
                                    style = Theme.typography.v5,
                                    color = Theme.color.primary
                                )
                                Text(text = item.name)
                            }
                        }
                    }
                }
            }

            // 链接
            val link = activity.link
            if (link.enabled) {
                Text(
                    text = "演出链接",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    link.showstart?.ifEmpty { null }?.let { showstart ->
                        LinkIcon("秀动", painterResource(Res.drawable.img_showstart)) {
                            launch {
                                val uri = Uri.parse(showstart)
                                if (uri == null) slot.tip.warning("链接已失效")
                                else if (!app.os.application.startAppIntent(uri)) slot.tip.warning("未安装秀动")
                            }
                        }
                    }
                    link.damai?.ifEmpty { null }?.let { damai ->
                        LinkIcon("大麦", painterResource(Res.drawable.img_damai)) {
                            navigateScreenWebPage("https://m.damai.cn/shows/item.html?itemId=${damai}")
                        }
                    }
                    link.maoyan?.ifEmpty { null }?.let { maoyan ->
                        LinkIcon("猫眼", painterResource(Res.drawable.img_maoyan)) {
                            navigateScreenWebPage("https://show.maoyan.com/qqw#/detail/${maoyan}")
                        }
                    }
                    link.link?.ifEmpty { null }?.let { link ->
                        LinkIcon("直播", rememberVectorPainter(Icons.Link)) {
                            navigateScreenWebPage(link)
                        }
                    }

                    val qqGroupUri = Platform.use(*Platform.Phone,
                        ifTrue = {
                            link.qqGroupPhone?.ifEmpty { null }?.let { group -> UriGenerator.qqGroup(group) }
                        },
                        ifFalse = {
                            link.qqGroupLink?.ifEmpty { null }?.let { q -> UriGenerator.qqGroupLink(q) }
                        }
                    )
                    qqGroupUri?.let { uri ->
                        LinkIcon("官群", rememberVectorPainter(Icons2.QQ)) {
                            launch {
                                if (!app.os.application.startAppIntent(uri)) slot.tip.warning("未安装QQ")
                            }
                        }
                    }
                }
            }

            // 开售时间

            if (activity.saleTime.isNotEmpty()) {
                Text(
                    text = "开售时间",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    activity.saleTime.fastForEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Theme.shape.v5,
                            contentPadding = Theme.padding.value9,
                            contentAlignment = Alignment.CenterStart,
                            shadowElevation = Theme.shadow.v3,
                            border = BorderStroke(Theme.border.v9, Theme.color.outline),
                            onClick = {}
                        ) {
                            SimpleEllipsisText(
                                text = "${index + 1}.  $item",
                                color = if (index == 0) Theme.color.tertiary else Theme.color.secondary,
                                style = Theme.typography.v7.bold
                            )
                        }
                    }
                }
            }

            // 服务说明
            activity.content?.ifEmpty { null }?.let { content ->
                Text(
                    text = "服务说明",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                SelectionBox {
                    Text(
                        text = content,
                        modifier = Modifier.padding(Theme.padding.value)
                    )
                }
            }
        }
    }

    @Composable
    private fun ActivityPhotoLayout(activity: Activity, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            // 演出阵容
            if (activity.lineup.isNotEmpty()) {
                Text(
                    text = "演出阵容",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                TagView(
                    size = activity.lineup.size,
                    titleProvider = { activity.lineup[it] },
                    readonly = true,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
                )
            }

            // 座位图
            val seatPath = activity.photo.seatPath?.url
            if (seatPath != null) {
                Text(
                    text = "座位图",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                WebImage(
                    uri = seatPath,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(vertical = Theme.padding.v)
                )
            }

            // 海报
            val pics = remember(activity) {
                activity.photo.posters.fastMap { Picture(activity.photo.posterPath(it).url) }
            }
            if (pics.isNotEmpty()) {
                Text(
                    text = "海报",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                NineGrid(
                    pics = pics,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Theme.padding.v),
                    onImageClick = { index, _ ->
                        navigate(::ScreenImagePreview, pics, index)
                    }
                ) { isSingle, pic, onClick ->
                    WebImage(
                        uri = pic.image,
                        contentScale = if (isSingle) ContentScale.Inside else ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().condition(!isSingle) { fillMaxHeight() },
                        onClick = onClick
                    )
                }
            }
        }
    }

    @Composable
    private fun ActivityPlaylistLayout(activity: Activity, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            // 歌单
            if (activity.playlist.isNotEmpty()) {
                Text(
                    text = "歌单",
                    style = Theme.typography.v7.bold,
                    modifier = Modifier.padding(Theme.padding.value)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    for (item in activity.playlist) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Theme.shape.v5,
                            contentPadding = Theme.padding.value9,
                            contentAlignment = Alignment.CenterStart,
                            shadowElevation = Theme.shadow.v3,
                            border = BorderStroke(Theme.border.v9, Theme.color.outline),
                            onClick = {}
                        ) {
                            SimpleEllipsisText(text = item, style = Theme.typography.v7.bold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(activity: Activity) {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().verticalScroll(rememberScrollState())) {
            ActivityInfoLayout(activity = activity, modifier = Modifier.fillMaxWidth())
            ActivityPhotoLayout(activity = activity, modifier = Modifier.fillMaxWidth())
            ActivityPlaylistLayout(activity = activity, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable
    private fun Landscape(activity: Activity) {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            ActivityInfoLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            Divider()
            ActivityPhotoLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            Divider()
            ActivityPlaylistLayout(activity = activity, modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (app.config.userProfile?.hasPrivilegeVIPCalendar == true) {
            Icon(icon = Icons.Edit, tip = "编辑", onClick = {
                navigate(::ScreenModifyActivity, aid)
            })
            LoadingIcon(icon = Icons.Delete, tip = "删除", onClick = {
                if (slot.confirm.open(content = "删除活动")) deleteActivity()
            })
        }
    }

    @Composable
    override fun Content() {
        targetActivity?.let {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT -> Portrait(it)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(it)
            }
        }
    }
}