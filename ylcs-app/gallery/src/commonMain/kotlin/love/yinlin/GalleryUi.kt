package love.yinlin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.ColorSystem
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text

internal val LocalGalleryCompact = staticCompositionLocalOf { false }

internal enum class GalleryPalette(val title: String) {
    Gallery("海盐配色"),
    Framework("框架默认"),
}

internal val GalleryColors = ColorSystem.Default.copy(
    light = ColorSystem.Default.light.copy(
        primary = Color(0xff137c83),
        primaryContainer = Color(0xff126b73),
        secondary = Color(0xff536f99),
        secondaryContainer = Color(0xff405675),
        tertiary = Color(0xffac6d57),
        tertiaryContainer = Color(0xff925743),
        background = Color(0xfff3f6fa),
        backgroundVariant = Color(0xffeaf0f5),
        surface = Color(0xffffffff),
        onBackground = Color(0xff172b43),
        onBackgroundVariant = Color(0xff52657a),
        onSurface = Color(0xff172b43),
        onSurfaceVariant = Color(0xff52657a),
        outline = Color(0xffbdcbd8),
    ),
    dark = ColorSystem.Default.dark.copy(
        primary = Color(0xff7bd7d9),
        primaryContainer = Color(0xff126b73),
        secondary = Color(0xffa9bee4),
        secondaryContainer = Color(0xff405675),
        tertiary = Color(0xffedb69f),
        tertiaryContainer = Color(0xff925743),
        background = Color(0xff101b2b),
        backgroundVariant = Color(0xff203044),
        surface = Color(0xff19283b),
        onBackground = Color(0xffe7eff8),
        onBackgroundVariant = Color(0xffb0bfd0),
        onSurface = Color(0xffe7eff8),
        onSurfaceVariant = Color(0xffb0bfd0),
        outline = Color(0xff53677f),
    ),
)

@Composable
internal fun GalleryParagraph(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 13.sp, lineHeight = 22.sp, color = Theme.color.onSurfaceVariant)
}

@Composable
internal fun GalleryBadge(text: String, color: Color = Theme.color.primary) {
    Box(Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(7.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(text, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
internal fun GalleryIconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.background(Theme.color.primary.copy(alpha = 0.09f), RoundedCornerShape(13.dp)).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, color = Theme.color.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun GalleryActions(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun GalleryCard(
    title: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Theme.color.outline.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(if (LocalGalleryCompact.current) 18.dp else 24.dp),
        contentAlignment = Alignment.TopStart,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (title != null) Text(title, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) GalleryParagraph(description)
            content()
        }
    }
}

@Composable
internal fun GalleryPageHeader(item: PageItem) {
    val number = (PageItem.entries.indexOf(item) + 1).toString().padStart(2, '0')
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("$number / RACHEL UI", fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, color = Theme.color.primary)
        GalleryActions {
            Text(item.title, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
            GalleryBadge(item.english)
            GalleryBadge("${item.components.size} 组展示", Theme.color.onSurfaceVariant)
        }
        Text(item.description, fontSize = 15.sp, lineHeight = 25.sp, color = Theme.color.onBackgroundVariant)
    }
}

@Composable
internal fun GallerySectionHeading(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
        GalleryParagraph(description)
    }
}

@Composable
internal fun GalleryMetric(label: String, value: String, detail: String, modifier: Modifier = Modifier) {
    GalleryCard(modifier = modifier) {
        Text(label, color = Theme.color.onSurfaceVariant, fontSize = 12.sp)
        Text(value, fontSize = 29.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        GalleryParagraph(detail)
    }
}

@Composable
internal fun GalleryCaption(text: String) {
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 18.sp, color = Theme.color.onSurfaceVariant)
}
