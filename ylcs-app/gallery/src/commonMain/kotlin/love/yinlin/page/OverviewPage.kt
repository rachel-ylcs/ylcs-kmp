package love.yinlin.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.*
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text

@Stable
internal object OverviewPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Hero()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else 3,
            ) {
                GalleryMetric("组件分类", PageItem.entries.size.toString(), "从主题与文本，到容器与浮窗。", Modifier.weight(1f))
                GalleryMetric("展示分组", PageItem.componentCount.toString(), "每组都保留实际控件与可操作示例。", Modifier.weight(1f))
                GalleryMetric("外观模式", "Light / Dark", "点击右上角，比较两种外观。", Modifier.weight(1f))
            }
            PageGroup.entries.forEach { group ->
                GallerySectionHeading(group.title, when (group) {
                    PageGroup.Foundation -> "从颜色角色与文字排版开始，建立一致的界面语言。"
                    PageGroup.Interaction -> "点击、拖动、切换与打开浮层，感受组件如何响应操作。"
                    PageGroup.Layout -> "把图片、内容与集合放在合适的位置，让信息自然铺开。"
                })
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else 2,
                ) {
                    PageItem.entries.filter { it.group == group }.forEach { item ->
                        GalleryCard(modifier = Modifier.weight(1f).heightIn(min = 238.dp), onClick = { currentPage = item.page }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                                GalleryIconTile(item.icon)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(item.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                    GalleryCaption(item.english)
                                }
                                GalleryBadge("${item.components.size} 组")
                            }
                            GalleryParagraph(item.description)
                            GalleryCaption(item.components.joinToString(" · ") { it.title })
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("打开示例", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Theme.color.primary)
                                Icon(Icons.KeyboardArrowRight, color = Theme.color.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            GalleryCard("从这里开始", "这些页面展示的是实际组件，可以直接输入、点击和拖动。") {
                GalleryActions {
                    GalleryBadge("01 · 选择一个分类")
                    GalleryBadge("02 · 操作真实控件")
                    GalleryBadge("03 · 比较浅色与深色")
                }
                GalleryParagraph("搜索支持中文分类名与组件名，例如 Button、NineGrid、Calendar。主题页还可以在海盐配色与框架默认配色之间切换。")
            }
        }
    }

    @Composable
    private fun Hero() {
        var enabled by remember { mutableStateOf(true) }
        var value by remember { mutableStateOf(0.65f) }
        var clicks by remember { mutableIntStateOf(0) }
        val compact = LocalGalleryCompact.current
        val primary = Theme.color.primary
        val secondary = Theme.color.secondary
        val surface = Theme.color.surface
        val gradient = Brush.linearGradient([
            surface,
            primary.copy(alpha = 0.09f).compositeOver(surface),
        ])
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(gradient)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(primary.copy(alpha = 0.06f), radius = 160.dp.toPx(), center = Offset(size.width, 0f))
                drawCircle(secondary.copy(alpha = 0.05f), radius = 120.dp.toPx(), center = Offset(size.width * 0.67f, size.height))
            }
            val heading: @Composable ColumnScope.() -> Unit = {
                GalleryBadge("RACHEL / COMPOSE MULTIPLATFORM")
                Text("每个组件，\n都值得亲手试试。", fontSize = if (compact) 30.sp else 36.sp,
                    lineHeight = if (compact) 41.sp else 48.sp, fontWeight = FontWeight.Bold)
                Text("用真实的交互认识 Rachel UI。\n在同一套界面里，探索颜色、布局与反馈。",
                    fontSize = 14.sp, lineHeight = 24.sp, color = Theme.color.onSurfaceVariant)
                GalleryActions {
                    PrimaryButton("从主题开始", icon = Icons.Theme) { currentPage = ThemePage }
                    SecondaryButton("探索输入", icon = Icons.CheckBox) { currentPage = InputPage }
                }
                GalleryCaption("DESIGNED TO BE EXPLORED  ·  DESKTOP / WEB")
            }
            val preview: @Composable (Modifier) -> Unit = { modifier ->
                GalleryCard(modifier = modifier) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("交互预览", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        GalleryBadge(if (enabled) "LIVE" else "PAUSED", if (enabled) Theme.color.primary else Theme.color.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启用控件", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Switch(enabled, { enabled = it })
                    }
                    Slider(value, { value = it }, onValueChange = { value = it }, enabled = enabled, modifier = Modifier.fillMaxWidth())
                    GalleryCaption("滑块值  ${(value * 100).toInt()}%")
                    PrimaryButton("点击了 $clicks 次", enabled = enabled, modifier = Modifier.fillMaxWidth()) { clicks++ }
                    TextButton("重置预览", icon = Icons.Refresh) { enabled = true; value = 0.65f; clicks = 0 }
                }
            }
            if (compact) Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                heading()
                preview(Modifier.fillMaxWidth())
            }
            else Row(Modifier.padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp), content = heading)
                preview(Modifier.width(282.dp))
            }
        }
    }
}
