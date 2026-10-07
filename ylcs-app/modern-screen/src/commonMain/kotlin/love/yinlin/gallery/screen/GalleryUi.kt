package love.yinlin.gallery.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.ClearPolicy
import love.yinlin.compose.screen.CreatePolicy
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.text.Text

@Stable
internal abstract class GalleryScreen(
    protected val routes: GalleryRoutes,
    private val section: GallerySection,
    name: String = section.title,
    initial: String = "无导航参数",
) : ObservedScreen(routes.journal, "Gallery", name, initial) {
    private fun select(target: GallerySection) {
        journal.event("Gallery", "navigate", "打开 ${target.title} · Move + Clear")
        val policy = CreatePolicy.Move + ClearPolicy.Clear
        when (target) {
            GallerySection.Overview -> navigate(routes::overview, policy)
            GallerySection.Policies -> navigate(routes::policies, policy)
            GallerySection.Arguments -> navigate(routes::arguments, policy)
            GallerySection.State -> navigate(routes::state, policy)
            GallerySection.Nested -> navigate(routes::nested, policy)
            GallerySection.Factories -> navigate(routes::factories, policy)
            GallerySection.Journal -> navigate(routes::records, policy)
        }
    }

    @Composable
    final override fun ScreenContent() {
        Theme.ThemeModeWrapper(routes.darkMode) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Theme.color.background)) {
                val wide = maxWidth >= 1060.dp
                Row(Modifier.fillMaxSize()) {
                    if (wide) {
                        Column(
                            Modifier.width(230.dp).fillMaxHeight().background(Theme.color.surface)
                                .padding(22.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Badge("RACHEL / COMPOSE")
                            Text("Screen\nGallery", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                            Paragraph("看见导航过程，理解页面状态。")
                            HorizontalDivider()
                            GallerySection.entries.forEach { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    tonalLevel = if (item == section) 2 else 0,
                                    contentAlignment = Alignment.CenterStart,
                                    contentPadding = PaddingValues(12.dp),
                                    onClick = { select(item) },
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(item.title, fontWeight = FontWeight.SemiBold,
                                            color = if (item == section) Theme.color.primary else Theme.color.onSurface)
                                        Text(item.caption, fontSize = 11.sp, lineHeight = 16.sp, color = Theme.color.onSurfaceVariant)
                                    }
                                }
                            }
                            HorizontalDivider()
                            Paragraph("本次会话")
                            Text("${journal.createdCount} 次构造 · ${journal.clearedCount} 次清理", fontSize = 12.sp)
                            Paragraph("Desktop 与 Web 共用此界面。实例编号来自演示记录。")
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().background(Theme.color.surface).padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Layers, color = Theme.color.primary, modifier = Modifier.size(22.dp))
                            Text("Modern Screen", fontWeight = FontWeight.SemiBold)
                            Badge("$displayName · $instanceId")
                            PrimaryTextButton("返回", icon = Icons.ArrowBack) {
                                val popped = pop()
                                journal.event("Gallery", "pop", "$displayName $instanceId · 返回 $popped")
                            }
                            TextButton("概览", icon = Icons.Home) { select(GallerySection.Overview) }
                            TextButton(if (routes.darkMode) "浅色" else "深色", icon = if (routes.darkMode) Icons.LightMode else Icons.DarkMode) {
                                routes.darkMode = !routes.darkMode
                            }
                        }
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                                .padding(if (wide) 28.dp else 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            if (!wide) {
                                Actions {
                                    GallerySection.entries.forEach { item ->
                                        TextButton(item.title, color = if (item == section) Theme.color.primary else Theme.color.onBackgroundVariant) { select(item) }
                                    }
                                }
                            }
                            PageContent()
                            if (section != GallerySection.Journal) {
                                GalleryCard("最近发生", "真实的构造、生命周期回调与组合事件。可在「观察记录」查看完整会话。") {
                                    EventRows(journal.events.takeLast(5).asReversed())
                                }
                            }
                            Paragraph("Screen Gallery · 实例记录不代表导航栈；出栈后的清理可能等待退出动画完成。")
                        }
                    }
                }
            }
        }
    }

    @Composable
    protected abstract fun PageContent()
}

@Composable
internal fun PageHeading(number: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("$number / SCREEN GALLERY", color = Theme.color.primary, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
        Text(title, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Theme.color.onBackgroundVariant, fontSize = 15.sp, lineHeight = 24.sp)
    }
}

@Composable
internal fun Paragraph(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 21.sp, color = Theme.color.onBackgroundVariant)
}

@Composable
internal fun Badge(text: String, color: Color = Theme.color.primary) {
    Box(Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Actions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
internal fun GalleryCard(title: String, description: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Theme.color.outline.copy(alpha = 0.35f)),
        contentPadding = PaddingValues(20.dp), contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) Paragraph(description)
            content()
        }
    }
}

@Composable
internal fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Theme.color.backgroundVariant, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Theme.color.onBackgroundVariant, fontSize = 12.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun CodeBlock(code: String) {
    SelectionContainer {
        Box(Modifier.fillMaxWidth().background(Theme.color.backgroundVariant, RoundedCornerShape(12.dp)).padding(16.dp)) {
            Text(code.trimIndent(), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
internal fun EventRows(events: List<GalleryEvent>) {
    if (events.isEmpty()) Paragraph("还没有事件。执行一次导航或返回后，记录会出现在这里。")
    events.forEach { event ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Actions {
                Text("${event.number.toString().padStart(3, '0')}  +${event.elapsed}", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                    color = Theme.color.onSurfaceVariant)
                Badge(event.kind)
                Text(event.scope, fontSize = 11.sp, color = Theme.color.onSurfaceVariant)
            }
            Text(event.message, fontSize = 13.sp, lineHeight = 20.sp)
            HorizontalDivider(color = Theme.color.outline.copy(alpha = 0.2f))
        }
    }
}
