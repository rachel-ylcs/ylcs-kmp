package love.yinlin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.MainScope
import love.yinlin.compose.ColorSystem
import love.yinlin.compose.Theme
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.layout.VerticalDivider
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.gallery.resources.*
import love.yinlin.page.OverviewPage

var darkMode: Boolean by mutableStateOf(false)
var currentPage: Page? by mutableRefStateOf(OverviewPage)
val mainScope = MainScope()
internal var galleryPalette: GalleryPalette by mutableStateOf(GalleryPalette.Gallery)

@Composable
fun App() {
    val search = rememberInputState()
    val holder = rememberSaveableStateHolder()
    val selected = currentPage?.let { PageItem.forPage(it) } ?: PageItem.overview
    val query = search.text.trim()
    val filtered = remember(query) { PageItem.allEntries.filter { it.matches(query) } }

    Theme(
        mainFontResource = Res.font.font,
        themeMode = if (darkMode) ThemeMode.DARK else ThemeMode.LIGHT,
        colorSystem = if (galleryPalette == GalleryPalette.Framework) ColorSystem.Default else GalleryColors,
        toolingTheme = remember { ToolingTheme(enableBallonTip = true) },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Theme.color.background)) {
            val wide = maxWidth >= 1080.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    GallerySidebar(selected, filtered, search)
                    VerticalDivider(color = Theme.color.outline.copy(alpha = 0.25f))
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    CompositionLocalProvider(LocalGalleryCompact provides (maxWidth < 820.dp)) {
                        Column(Modifier.fillMaxSize()) {
                            GalleryToolbar(selected, wide, filtered, search)
                            HorizontalDivider(color = Theme.color.outline.copy(alpha = 0.25f))
                            AnimationContent(
                                state = selected.id,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            ) { id ->
                                val item = PageItem.allEntries.first { it.id == id }
                                holder.SaveableStateProvider(id) {
                                    item.page.Content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GallerySidebar(selected: PageItem, filtered: List<PageItem>, search: InputState) {
    Column(Modifier.width(256.dp).fillMaxHeight().background(Theme.color.surface)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GalleryIconTile(Icons.Layers)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RACHEL", fontSize = 11.sp, letterSpacing = 2.sp, color = Theme.color.primary, fontWeight = FontWeight.SemiBold)
                    Text("UI Gallery", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            GalleryParagraph("看见组件的样子，试一试它的交互。")
            GallerySearch(search)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (PageItem.overview in filtered) item(key = "overview") { GalleryNavItem(PageItem.overview, selected) }
            PageGroup.entries.forEach { group ->
                val pages = filtered.filter { it.group == group }
                if (pages.isNotEmpty()) {
                    item(key = group.name) {
                        Text(group.title, modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 5.dp),
                            fontSize = 11.sp, letterSpacing = 1.sp, color = Theme.color.onSurfaceVariant)
                    }
                    items(pages, key = { it.id }) { GalleryNavItem(it, selected) }
                }
            }
            if (filtered.isEmpty()) item(key = "empty") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GalleryParagraph("没有找到匹配的组件。试试 Button、输入或日历。")
                    TextButton("清除搜索", icon = Icons.Clear) { search.clear() }
                }
            }
        }
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(color = Theme.color.outline.copy(alpha = 0.25f))
            GalleryCaption("${PageItem.entries.size} 个分类  /  ${PageItem.componentCount} 组展示")
            GalleryActions {
                GalleryBadge("Desktop")
                GalleryBadge("Web")
            }
        }
    }
}

@Composable
private fun GalleryNavItem(item: PageItem, selected: PageItem) {
    val active = item.id == selected.id
    Surface(
        modifier = Modifier.fillMaxWidth().semantics {
            this.selected = active
            role = Role.Tab
        },
        shape = RoundedCornerShape(12.dp),
        tonalLevel = if (active) 2 else 0,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
        contentAlignment = Alignment.CenterStart,
        onClick = { currentPage = item.page },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(item.icon, color = if (active) Theme.color.primary else Theme.color.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) Theme.color.primary else Theme.color.onSurface)
                Text(item.english, fontSize = 10.sp, lineHeight = 15.sp, color = Theme.color.onSurfaceVariant)
            }
            if (item.components.isNotEmpty()) Text(item.components.size.toString(), fontSize = 11.sp, color = Theme.color.onSurfaceVariant)
        }
    }
}

@Composable
private fun GallerySearch(state: InputState) {
    Input(
        state = state,
        hint = "搜索组件或分类",
        leading = InputDecoration.Icon(icon = { Icons.Search }),
        trailing = InputDecoration.Icon.Clear,
        style = Theme.typography.v6,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GalleryToolbar(selected: PageItem, wide: Boolean, filtered: List<PageItem>, search: InputState) {
    Column(
        Modifier.fillMaxWidth().background(Theme.color.surface).padding(horizontal = if (wide) 28.dp else 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!wide) Icon(Icons.Layers, color = Theme.color.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (wide) "组件库 / ${selected.title}" else "Rachel UI · ${selected.title}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (selected.components.isEmpty()) "可交互的 Compose 组件展示" else selected.english,
                    fontSize = 11.sp, color = Theme.color.onSurfaceVariant)
            }
            if (wide) GalleryBadge(galleryPalette.title)
            TextButton(if (darkMode) "浅色" else "深色", icon = if (darkMode) Icons.LightMode else Icons.DarkMode) {
                darkMode = !darkMode
            }
        }
        if (!wide) {
            GallerySearch(search)
            if (filtered.isEmpty()) GalleryParagraph("没有匹配的组件，可以修改或清除搜索。")
            else Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filtered.forEach { item ->
                    Surface(
                        modifier = Modifier.semantics {
                            this.selected = item.id == selected.id
                            role = Role.Tab
                        },
                        shape = RoundedCornerShape(10.dp),
                        tonalLevel = if (item.id == selected.id) 2 else 0,
                        border = BorderStroke(1.dp, Theme.color.outline.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { currentPage = item.page },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(item.icon, color = if (item.id == selected.id) Theme.color.primary else Theme.color.onSurfaceVariant,
                                modifier = Modifier.size(16.dp))
                            Text(item.title, fontSize = 12.sp,
                                color = if (item.id == selected.id) Theme.color.primary else Theme.color.onSurface)
                        }
                    }
                }
            }
        }
    }
}
