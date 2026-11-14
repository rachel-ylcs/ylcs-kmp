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
internal object OverviewPage : Page(
    id = "overview",
    icon = Icons.Home,
    title = "总览",
    english = "Overview",
    description = "浏览 Rachel UI 的组件和交互示例。",
    keywords = "组件库",
) {
    @Composable
    override fun Content() {
        ComponentColumn {
            Hero()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else 3,
            ) {
                GalleryMetric("组件分类", GalleryCatalog.entries.size.toString(), Modifier.weight(1f))
                GalleryMetric("组件示例", GalleryCatalog.componentCount.toString(), Modifier.weight(1f))
                GalleryMetric("当前外观", if (darkMode) "深色" else "浅色", Modifier.weight(1f))
            }
            PageGroup.entries.forEach { group ->
                GallerySectionHeading(group.title, group.description)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else 2,
                ) {
                    GalleryCatalog.entries.filter { it.group == group }.forEach { item ->
                        GalleryCard(modifier = Modifier.weight(1f).heightIn(min = 238.dp), onClick = { currentPage = item }) {
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
                Text("Rachel UI", fontSize = if (compact) 30.sp else 36.sp,
                    lineHeight = if (compact) 41.sp else 48.sp, fontWeight = FontWeight.Bold)
                Text(description,
                    fontSize = 14.sp, lineHeight = 24.sp, color = Theme.color.onSurfaceVariant)
                GalleryActions {
                    PrimaryButton(ThemePage.title, icon = ThemePage.icon) { currentPage = ThemePage }
                    SecondaryButton(InputPage.title, icon = InputPage.icon) { currentPage = InputPage }
                }
            }
            val preview: @Composable (Modifier) -> Unit = { modifier ->
                GalleryCard(modifier = modifier) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("交互预览", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        GalleryBadge(if (enabled) "LIVE" else "PAUSED", if (enabled) Theme.color.primary else Theme.color.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("启用控件", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Switch(enabled, onCheckedChange = { enabled = it })
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
