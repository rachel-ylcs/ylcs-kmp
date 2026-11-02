package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.Text

private val LocalGallerySelectedComponentIndex = staticCompositionLocalOf { -1 }

@Stable
abstract class Page {
    @Composable
    abstract fun Content()

    @Composable
    protected fun ComponentColumn(alwaysShowTabs: Boolean = false, showTabs: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
        val compact = LocalGalleryCompact.current
        val components = PageItem.forPage(this@Page)?.components.orEmpty()
        val selectedComponent = rememberSaveable { mutableIntStateOf(0) }
        val selectedIndex = if (!showTabs || components.isEmpty()) -1 else selectedComponent.intValue.coerceIn(0, components.lastIndex)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 1240.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PageItem.forPage(this@Page)?.takeIf { it.components.isNotEmpty() }?.let { GalleryPageHeader(it) }
                if (showTabs && (components.size > 1 || (alwaysShowTabs && components.isNotEmpty()))) {
                    TabBar(
                        size = components.size,
                        index = selectedIndex,
                        onNavigate = { selectedComponent.intValue = it },
                        titleProvider = { components[it].title },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CompositionLocalProvider(LocalGallerySelectedComponentIndex provides selectedIndex) {
                    content()
                }
                HorizontalDivider(color = Theme.color.outline.copy(alpha = 0.25f))
                GalleryCaption("RACHEL UI GALLERY  /  DESKTOP · WEB")
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    @Composable
    protected fun ColumnScope.Component(title: String, content: @Composable ColumnScope.() -> Unit) {
        val item = PageItem.forPage(this@Page)
        val index = item?.components?.indexOfFirst { it.title == title } ?: -1
        val selectedIndex = LocalGallerySelectedComponentIndex.current
        if (selectedIndex < 0 || index < 0 || index == selectedIndex) {
            GalleryCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (index >= 0) GalleryBadge((index + 1).toString().padStart(2, '0'))
                    Text(title, fontSize = 19.sp, lineHeight = 27.sp)
                }
                if (index >= 0) GalleryParagraph(item!!.components[index].description)
                content()
            }
        }
    }

    @Composable
    protected fun ExampleRow(maxItemsInEachRow: Int = Int.MAX_VALUE, content: @Composable FlowRowScope.() -> Unit) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.Top,
            maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else maxItemsInEachRow,
            content = content,
        )
    }

    @Composable
    protected fun Example(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        val shape = RoundedCornerShape(13.dp)
        Column(
            modifier = modifier.then(if (LocalGalleryCompact.current) Modifier.fillMaxWidth() else Modifier)
                .widthIn(min = 144.dp)
                .clip(shape).border(1.dp, Theme.color.outline.copy(alpha = 0.22f), shape)
                .background(Theme.color.backgroundVariant.copy(alpha = 0.55f)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
            GalleryCaption(title)
        }
    }
}
