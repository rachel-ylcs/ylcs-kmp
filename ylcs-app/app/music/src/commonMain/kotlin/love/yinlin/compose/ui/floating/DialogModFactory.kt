package love.yinlin.compose.ui.floating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText

class DialogModFactory : Dialog<DialogModFactory.ModResult>() {
    @Stable
    sealed interface ModResult {
        @Stable
        data object FromCenter : ModResult
        @Stable
        data object FromImport : ModResult
        @Stable
        data object FromCreate : ModResult
        @Stable
        data object FromPlatform : ModResult
    }

    @Composable
    private fun ModFactoryItem(
        result: ModResult,
        text: String,
        textColor: Color,
        icon: ImageVector,
        iconColor: Color
    ) {
        Column(
            modifier = Modifier.clickable { future?.send(result) }.padding(Theme.padding.eValue9),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            Icon(
                icon = icon,
                color = iconColor,
                modifier = Modifier.size(Theme.size.image9)
            )
            SimpleClipText(
                text = text,
                color = textColor,
                style = Theme.typography.v6.bold
            )
        }
    }

    suspend fun open(): ModResult? = awaitResult()

    @Composable
    override fun Land() {
        LandDialog {
            Layout(
                content = {
                    ModFactoryItem(
                        result = ModResult.FromCenter,
                        text = "银临合集",
                        textColor = Theme.color.primary,
                        icon = Icons.LibraryMusic,
                        iconColor = Theme.color.primary,
                    )
                    ModFactoryItem(
                        result = ModResult.FromImport,
                        text = "本地导入",
                        textColor = Theme.color.secondary,
                        icon = Icons.Upload,
                        iconColor = Theme.color.secondary
                    )
                    ModFactoryItem(
                        result = ModResult.FromCreate,
                        text = "本地创建",
                        textColor = Theme.color.tertiary,
                        icon = Icons.DesignServices,
                        iconColor = Theme.color.tertiary
                    )
                    ModFactoryItem(
                        result = ModResult.FromPlatform,
                        text = "平台音乐",
                        textColor = Theme.color.primary,
                        icon = Icons.CloudDownload,
                        iconColor = Theme.color.primary,
                    )
                }
            ) { measurables, constraints ->
                val maxRequestedWidth = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
                val columns = 2
                val slotWidth = if (constraints.hasBoundedWidth) maxRequestedWidth.coerceAtMost(constraints.maxWidth / columns) else maxRequestedWidth
                val totalWidth = slotWidth * columns
                val exactConstraints = Constraints(minWidth = slotWidth, maxWidth = slotWidth, minHeight = 0, maxHeight = constraints.maxHeight)
                val placeables = measurables.map { it.measure(exactConstraints) }
                val childHeight = placeables.first().height
                val rowCount = (placeables.size + columns - 1) / columns
                val totalHeight = rowCount * childHeight
                layout(totalWidth, totalHeight) {
                    placeables.forEachIndexed { index, placeable ->
                        val row = index / columns
                        val column = index % columns
                        placeable.placeRelative(x = column * slotWidth, y = row * childHeight)
                    }
                }
            }
        }
    }
}
