package love.yinlin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.text.Text

@Stable
abstract class Page {
    @Composable
    abstract fun Content()

    @Composable
    protected fun ComponentColumn(content: @Composable ColumnScope.() -> Unit) {
        val compact = LocalGalleryCompact.current
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 1240.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PageItem.forPage(this@Page)?.takeIf { it.components.isNotEmpty() }?.let { GalleryPageHeader(it) }
                content()
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
        GalleryCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (index >= 0) GalleryBadge((index + 1).toString().padStart(2, '0'))
                Text(title, fontSize = 19.sp, lineHeight = 27.sp)
            }
            if (index >= 0) GalleryParagraph(item!!.components[index].description)
            content()
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
