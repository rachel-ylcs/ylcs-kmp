package love.yinlin.compose.ui.common

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
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.data.music.PlatformMusicType

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
        data class FromPlatform(val type: PlatformMusicType) : ModResult
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
                    PlatformMusicType.entries.fastForEach { type ->
                        ModFactoryItem(
                            result = ModResult.FromPlatform(type),
                            text = type.description,
                            textColor = type.color,
                            icon = type.icon,
                            iconColor = Colors.Unspecified
                        )
                    }
                }
            ) { measurables, constraints ->
                val maxRequestedWidth = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
                val slotWidth = if (constraints.hasBoundedWidth) maxRequestedWidth.coerceAtMost(constraints.maxWidth / 3) else maxRequestedWidth
                val totalWidth = slotWidth * 3
                val exactConstraints = Constraints(minWidth = slotWidth, maxWidth = slotWidth, minHeight = 0, maxHeight = constraints.maxHeight)
                val placeables = measurables.map { it.measure(exactConstraints) }
                val childHeight = placeables.first().height
                val rowCount = (placeables.size + 2) / 3
                val totalHeight = rowCount * childHeight
                layout(totalWidth, totalHeight) {
                    placeables.forEachIndexed { index, placeable ->
                        val row = index / 3
                        val column = index % 3
                        placeable.placeRelative(x = column * slotWidth, y = row * childHeight)
                    }
                }
            }
        }
    }
}