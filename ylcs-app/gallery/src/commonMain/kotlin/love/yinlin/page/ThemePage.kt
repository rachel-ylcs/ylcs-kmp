package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.*
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Text

@Stable
object ThemePage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            GalleryCard("选择你的外观", "下方色块来自当前主题。切换配色或明暗模式，可以即时对照整个界面。") {
                GalleryActions {
                    GalleryPalette.entries.forEach { palette ->
                        TextButton(palette.title, icon = if (galleryPalette == palette) Icons.Check else Icons.Theme,
                            color = if (galleryPalette == palette) Theme.color.primary else Theme.color.onSurfaceVariant) {
                            galleryPalette = palette
                        }
                    }
                    TextButton(if (darkMode) "切换浅色" else "切换深色", icon = if (darkMode) Icons.LightMode else Icons.DarkMode) {
                        darkMode = !darkMode
                    }
                }
                GalleryBadge("${galleryPalette.title} · ${if (darkMode) "深色外观" else "浅色外观"}")
            }
            Component("Accent") {
                SwatchRow {
                    Swatch("primary", Theme.color.primary, modifier = Modifier.weight(1f))
                    Swatch("secondary", Theme.color.secondary, modifier = Modifier.weight(1f))
                    Swatch("tertiary", Theme.color.tertiary, modifier = Modifier.weight(1f))
                }
            }
            Component("Container") {
                val roles = ["onContainer" to Theme.color.onContainer, "onContainerVariant" to Theme.color.onContainerVariant]
                SwatchRow {
                    Swatch("primaryContainer", Theme.color.primaryContainer, roles, Modifier.weight(1f))
                    Swatch("secondaryContainer", Theme.color.secondaryContainer, roles, Modifier.weight(1f))
                    Swatch("tertiaryContainer", Theme.color.tertiaryContainer, roles, Modifier.weight(1f))
                }
            }
            Component("Background / Surface") {
                val backgroundRoles = ["onBackground" to Theme.color.onBackground, "onBackgroundVariant" to Theme.color.onBackgroundVariant]
                SwatchRow {
                    Swatch("background", Theme.color.background, backgroundRoles, Modifier.weight(1f))
                    Swatch("backgroundVariant", Theme.color.backgroundVariant, backgroundRoles, Modifier.weight(1f))
                    Swatch("surface", Theme.color.surface, ["onSurface" to Theme.color.onSurface, "onSurfaceVariant" to Theme.color.onSurfaceVariant], Modifier.weight(1f))
                }
            }
            Component("Status") {
                SwatchRow {
                    Swatch("error", Theme.color.error, ["onError" to Theme.color.onError], Modifier.weight(1f))
                    Swatch("warning", Theme.color.warning, ["onWarning" to Theme.color.onWarning], Modifier.weight(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth().height(114.dp).clip(RoundedCornerShape(13.dp)).background(Theme.color.backgroundVariant),
                            contentAlignment = Alignment.Center) {
                            Text("内容上的遮罩", fontSize = 14.sp, color = Theme.color.onBackground)
                            Box(Modifier.matchParentSize().background(Theme.color.scrim.copy(alpha = 0.5f)))
                        }
                        Text("scrim", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        GalleryCaption("${Theme.color.scrim.hex()} · 50% opacity")
                    }
                }
            }
            Component("Outline / Disabled") {
                SwatchRow {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(114.dp).border(2.dp, Theme.color.outline, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("outline", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("outline", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        GalleryCaption(Theme.color.outline.hex())
                    }
                    Swatch("disabledContainer", Theme.color.disabledContainer,
                        ["disabledContent" to Theme.color.disabledContent], Modifier.weight(1f))
                }
            }
        }
    }

    @Composable
    private fun SwatchRow(content: @Composable FlowRowScope.() -> Unit) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            maxItemsInEachRow = if (LocalGalleryCompact.current) 1 else 3,
            content = content,
        )
    }

    @Composable
    private fun Swatch(
        name: String,
        color: Color,
        roles: List<Pair<String, Color>> = [],
        modifier: Modifier = Modifier,
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 114.dp).clip(RoundedCornerShape(13.dp))
                    .background(color).border(1.dp, Theme.color.outline.copy(alpha = 0.2f), RoundedCornerShape(13.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    roles.forEach { [label, contentColor] ->
                        Text(label, color = contentColor, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            GalleryCaption(color.hex())
        }
    }

    private fun Color.hex(): String = "#${toArgb().toUInt().toString(16).padStart(8, '0').uppercase()}"
}
