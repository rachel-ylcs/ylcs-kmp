package love.yinlin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.MainScope
import love.yinlin.compose.Theme
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.text.Text
import love.yinlin.gallery.resources.*
import love.yinlin.page.*

var darkMode: Boolean by mutableStateOf(false)
var currentPage: Page? by mutableRefStateOf(ThemePage)
val mainScope = MainScope()

@Stable
data class PageItem(
    val icon: ImageVector,
    val text: String,
    val page: Page? = null,
    val content: (@Composable () -> Unit)? = null
) {
    companion object {
        val entries = listOf(
            PageItem(Icons.Theme, "主题", page = ThemePage),
            PageItem(Icons.TextFields, "文本", page = TextPage),
            PageItem(Icons.CheckBox, "输入", page = InputPage),
            PageItem(Icons.Image, "图片", page = ImagePage),
            PageItem(Icons.Animation, "动画", page = AnimationPage),
            PageItem(Icons.Package, "容器", page = ContainerPage),
            PageItem(Icons.GridOn, "集合", page = CollectionPage),
            PageItem(Icons.Anchor, "导航", page = NavigationPage),
            PageItem(Icons.Cloud, "浮窗", page = FloatingPage),
            PageItem(Icons.Token, "组件", page = WidgetPage),
            PageItem(Icons.LightMode, "深色模式", content = {
                Switch(
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }),
        )
    }
}

@Composable
fun App() {
    Theme(
        mainFontResource = Res.font.font,
        themeMode = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT,
        toolingTheme = remember { ToolingTheme(enableBallonTip = true) }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val primaryColor = Theme.color.primary
            LazyColumn(modifier = Modifier.width(Theme.size.cell2).fillMaxHeight()) {
                items(
                    items = PageItem.entries,
                    key = { it },
                ) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { if (item.page != null) currentPage = item.page }
                            .drawBehind {
                                if (currentPage == item.page) drawLine(primaryColor, Offset(4f, 8f), Offset(4f, size.height - 8f), 4f)
                            }.padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon = item.icon)
                        Text(text = item.text)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (item.content != null) item.content()
                            else Icon(icon = Icons.KeyboardArrowRight)
                        }
                    }
                }
            }
            Divider()
            AnimationContent(
                state = currentPage,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { page ->
                page?.Content()
            }
        }
    }
}