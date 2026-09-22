package love.yinlin.compose.ui.common

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.SimpleEllipsisText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
data class PortalCardItem(
    private val eyebrow: String,
    private val title: String,
    private val subtitle: String,
    private val drawable: DrawableResource,
    private val lightColors: List<Color>,
    private val darkColors: List<Color>,
    private val onClick: () -> Unit
) {
    private val lightBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            lightColors[0].copy(alpha = 0.22f),
            lightColors[1].copy(alpha = 0.96f),
            lightColors[2].copy(alpha = 0.98f)
        )
    )

    private val darkBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            darkColors[0].copy(alpha = 0.22f),
            darkColors[1].copy(alpha = 0.96f),
            darkColors[2].copy(alpha = 0.98f)
        )
    )

    private val lightBorderBrush = Brush.linearGradient(
        colors = listOf(
            lightColors[0].copy(alpha = 0.75f),
            Colors.Dark.copy(alpha = 0.25f),
            Colors.Transparent
        )
    )

    private val darkBorderBrush = Brush.linearGradient(
        colors = listOf(
            darkColors[0].copy(alpha = 0.75f),
            Colors.White.copy(alpha = 0.25f),
            Colors.Transparent
        )
    )

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val shape = Theme.shape.v8

        val darkMode = Theme.darkMode
        val backgroundBrush = if (darkMode) darkBackgroundBrush else lightBackgroundBrush
        val borderBrush = if (darkMode) darkBorderBrush else lightBorderBrush

        Box(
            modifier = modifier.clip(shape).background(
                brush = backgroundBrush,
                shape = shape
            ).border(
                width = Theme.border.v8,
                brush = borderBrush,
                shape = shape
            ).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(drawable),
                modifier = Modifier.matchParentSize().zIndex(1f),
                alignment = Alignment.TopEnd,
                contentScale = ContentScale.FillHeight,
                alpha = 0.75f
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue).zIndex(2f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.Bottom),
            ) {
                Space(Theme.padding.v5)
                SimpleEllipsisText(text = title, style = Theme.typography.v6.bold, color = Theme.color.onBackground)
                SimpleEllipsisText(text = subtitle, style = Theme.typography.v8, color = Theme.color.onBackgroundVariant)
            }
        }
    }
}