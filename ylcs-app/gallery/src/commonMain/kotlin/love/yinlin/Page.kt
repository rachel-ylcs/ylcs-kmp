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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.extension.staticLocalComposition
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.Text

private val LocalGallerySelectedComponent = staticLocalComposition<ComponentItem?> { null }

@Stable
abstract class Page internal constructor(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val english: String,
    val description: String,
    val keywords: String,
    internal val group: PageGroup? = null,
) {
    internal val components: List<ComponentItem> field = mutableListOf<ComponentItem>()

    internal fun component(title: String, description: String, keywords: String, previewAlias: String? = null): ComponentItem = ComponentItem(title, description, keywords, previewAlias).also { components.add(it) }

    @Composable
    abstract fun Content()

    @Composable
    protected fun ComponentColumn(alwaysShowTabs: Boolean = false, showTabs: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
        val compact = LocalGalleryCompact.current
        val previewIndex = components.indexOfFirst { it === galleryPreviewComponent }
        val selectedComponent = rememberValueState(previewIndex.coerceAtLeast(0))
        val selectedIndex = when {
            previewIndex >= 0 && !showTabs -> previewIndex
            showTabs && components.isNotEmpty() -> selectedComponent.intValue.coerceIn(0, components.lastIndex)
            else -> -1
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.widthIn(max = 1240.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (components.isNotEmpty()) GalleryPageHeader(this@Page)

                if (showTabs && (components.size > 1 || (alwaysShowTabs && components.isNotEmpty()))) {
                    TabBar(
                        size = components.size,
                        index = selectedIndex,
                        onNavigate = { selectedComponent.intValue = it },
                        titleProvider = { components[it].title },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                CompositionLocalProvider(LocalGallerySelectedComponent provides components.getOrNull(selectedIndex)) {
                    content()
                }

                HorizontalDivider(color = Theme.color.outline.copy(alpha = 0.25f))
                GalleryCaption("RACHEL UI GALLERY  /  DESKTOP · WEB")
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    @Composable
    internal fun ColumnScope.Component(component: ComponentItem, content: @Composable ColumnScope.() -> Unit) {
        val selected = LocalGallerySelectedComponent.current
        if (selected == null || component === selected) {
            GalleryCard {
                Text(component.title, fontSize = 19.sp, lineHeight = 27.sp)
                GalleryParagraph(component.description)
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
            modifier = modifier
                .then(if (LocalGalleryCompact.current) Modifier.fillMaxWidth() else Modifier)
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
