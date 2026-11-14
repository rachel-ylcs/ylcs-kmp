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
object ThemePage : Page(
    id = "theme",
    icon = Icons.Theme,
    title = "主题",
    english = "Theme",
    description = "用主题色统一界面的背景、内容和反馈。",
    keywords = "配色 明暗 ColorSystem",
    group = PageGroup.Foundation,
) {
    private val demoAccent = component("Accent", "用强调色区分操作的优先级。", "primary secondary tertiary")
    private val demoContainer = component("Container", "搭配容器底色与前景色，让文字清晰可读。", "primaryContainer onContainer")
    private val demoBackground = component("底色", "用背景和表面色区分页面与卡片。", "Surface backgroundVariant", previewAlias = "Background / Surface")
    private val demoStatus = component("Status", "用颜色区分错误、警告和背景遮罩。", "error warning scrim")
    private val demoOutline = component("轮廓", "用轮廓标出边界，用低对比色表示禁用。", "Outline Disabled", previewAlias = "Outline / Disabled")

    @Composable
    override fun Content() {
        ComponentColumn(showTabs = false) {
            GalleryCard(title = "配色") {
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
            Component(demoAccent) {
                SwatchRow {
                    Swatch("primary", Theme.color.primary, modifier = Modifier.weight(1f))
                    Swatch("secondary", Theme.color.secondary, modifier = Modifier.weight(1f))
                    Swatch("tertiary", Theme.color.tertiary, modifier = Modifier.weight(1f))
                }
            }
            Component(demoContainer) {
                val roles = ["onContainer" to Theme.color.onContainer, "onContainerVariant" to Theme.color.onContainerVariant]
                SwatchRow {
                    Swatch("primaryContainer", Theme.color.primaryContainer, roles = roles, modifier = Modifier.weight(1f))
                    Swatch("secondaryContainer", Theme.color.secondaryContainer, roles = roles, modifier = Modifier.weight(1f))
                    Swatch("tertiaryContainer", Theme.color.tertiaryContainer, roles = roles, modifier = Modifier.weight(1f))
                }
            }
            Component(demoBackground) {
                val backgroundRoles = ["onBackground" to Theme.color.onBackground, "onBackgroundVariant" to Theme.color.onBackgroundVariant]
                SwatchRow {
                    Swatch("background", Theme.color.background, roles = backgroundRoles, modifier = Modifier.weight(1f))
                    Swatch("backgroundVariant", Theme.color.backgroundVariant, roles = backgroundRoles, modifier = Modifier.weight(1f))
                    Swatch("surface", Theme.color.surface, roles = ["onSurface" to Theme.color.onSurface, "onSurfaceVariant" to Theme.color.onSurfaceVariant], modifier = Modifier.weight(1f))
                }
            }
            Component(demoStatus) {
                SwatchRow {
                    Swatch("error", Theme.color.error, roles = ["onError" to Theme.color.onError], modifier = Modifier.weight(1f))
                    Swatch("warning", Theme.color.warning, roles = ["onWarning" to Theme.color.onWarning], modifier = Modifier.weight(1f))
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
            Component(demoOutline) {
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
                        roles = ["disabledContent" to Theme.color.disabledContent], modifier = Modifier.weight(1f))
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
        modifier: Modifier = Modifier,
        roles: List<Pair<String, Color>> = [],
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
