package love.yinlin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.AdaptiveGrid
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import love.yinlin.extension.then
import love.yinlin.tpl.music.PlatformMusicManager
import love.yinlin.uri.Uri

@Stable
class ScreenPlatformMusic(deeplink: Uri? = null, platformType: PlatformMusicType? = null) : Screen() {
    @Stable
    private data class Selection(val input: String, val manager: PlatformMusicManager?)

    private val input = InputState(deeplink?.toString().orEmpty(), maxLength = 1024, imeAction = ImeAction.Search)
    private var selection by mutableStateOf(platformType?.let { Selection(input.text, PlatformMusicManager[it]) })

    private val recognition by derivedStateOf {
        PlatformMusicManager.recognize(input.text)
    }

    private val selected by derivedStateOf {
        val choice = selection
        if (choice != null && choice.input == input.text) choice.manager else recognition?.manager
    }

    private val searchManager by derivedStateOf {
        selected ?: recognition?.manager ?: PlatformMusicManager[PlatformMusicType.QQMusic]
    }

    private val query by derivedStateOf {
        val manager = searchManager
        val recognized = recognition
        if (recognized != null && recognized.manager == manager) recognized.query else manager.query(input.text)
    }

    private var searching by mutableStateOf(false)
    private var resultManager by mutableStateOf<PlatformMusicManager?>(null)
    private val results = mutableStateListOf<PlatformMusicInfo>()
    private val importing = mutableStateMapOf<String, Unit>()

    private val gridState = LazyGridState()

    override val title: String = "平台音乐"

    private fun search() {
        if (searching) return

        val manager = searchManager
        val query = query ?: return

        searching = true
        resultManager = manager
        results.clear()
        gridState.requestScrollToItem(0)

        launch {
            cleaning({
                catchingError {
                    manager.search(query, results)
                }.warningTip
            }) {
                searching = false
            }
        }
    }

    private fun PlatformMusicManager.download(id: String, track: PlatformMusicInfo) {
        if (id !in DataSourceMusic.library && importing.put(id, Unit) == null) {
            launch {
                cleaning({
                    catchingError {
                        DataSourceMusic.importPlatformTrack(this@download, track)
                    }.warningTip
                }) {
                    importing.remove(id)
                }
            }
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (searching) CircleLoading.Content()
        else Icon(icon = Icons.Search, enabled = query != null, onClick = ::search)
    }

    @Composable
    private fun SearchControls() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
        ) {
            Input(
                state = input,
                hint = "歌曲/歌手/ID/分享链接",
                modifier = Modifier.fillMaxWidth(),
                trailing = if (input.isNotEmpty) InputDecoration.Icon.Clear else null,
                onImeClick = {
                    search()
                    true
                }
            )

            AdaptiveGrid(
                minCellWidth = Theme.size.cell4,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (manager in PlatformMusicManager.All) {
                    PlatformCard(manager, Modifier.fillMaxWidth())
                }
            }

            SimpleEllipsisText(text = "规则", style = Theme.typography.v5.bold)

            val activeQuery = query
            val match = activeQuery?.match
            val matchText: String? = when {
                match != null -> "${match.rule.description} - ${match.value}"
                activeQuery != null -> "关键词 - ${activeQuery.keyword}"
                else -> null
            }

            SimpleEllipsisText(text = matchText ?: "未匹配", color = if (matchText == null) Theme.color.warning else Theme.color.primary)

            SimpleEllipsisText(text = "搜索结果", style = Theme.typography.v5.bold)
        }
    }

    @Stable
    data class PlatformMusicMetadata(val icon: ImageVector, val color: Color, val commonBrush: Brush, val activeBrush: Brush) {
        constructor(icon: ImageVector, color: Color) : this(
            icon = icon,
            color = color,
            commonBrush = Brush.horizontalGradient([color.copy(alpha = 0.11f), color.copy(alpha = 0.03f)]),
            activeBrush = Brush.horizontalGradient([color.copy(alpha = 0.2f), color.copy(alpha = 0.03f)])
        )
    }

    private val platformMetadata = mapOf(
        PlatformMusicType.QQMusic to PlatformMusicMetadata(Icons2.QQMusic, Colors.Yellow4),
        PlatformMusicType.NetEaseCloud to PlatformMusicMetadata(Icons2.NetEaseCloudMusic, Colors.Red4),
        PlatformMusicType.Kugou to PlatformMusicMetadata(Icons2.KugouMusic, Colors.Blue4),
        PlatformMusicType.Soda to PlatformMusicMetadata(Icons2.SodaMusic, Colors.Green4),
        PlatformMusicType.Migu to PlatformMusicMetadata(Icons2.MiguMusic, Colors.Pink4),
    )

    @Composable
    private fun PlatformCard(manager: PlatformMusicManager, modifier: Modifier = Modifier) {
        val active by rememberDerivedState { selected == manager || (searchManager == manager && query?.keyword?.isNotEmpty() == true) }
        val [icon, accentColor, commonBrush, activeBrush] = platformMetadata[manager.type]!!
        val shape = Theme.shape.v7

        Row(
            modifier = modifier.clip(shape)
                .background(if (active) activeBrush else commonBrush)
                .border(Theme.border.v7, if (active) accentColor else accentColor.copy(alpha = 0.24f), shape)
                .selectable(selected = active, role = Role.Button, onClick = {
                    selection = Selection(input.text, manager.takeUnless { it == selected })
                }).padding(Theme.padding.value9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon, color = Colors.Unspecified, modifier = Modifier.size(Theme.size.icon))

            SimpleEllipsisText(text = manager.type.description, color = Theme.color.onSurface, style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))

            if (active) Icon(icon = Icons.Check, color = accentColor, modifier = Modifier.size(Theme.size.smallIcon))
        }
    }

    @Composable
    private fun ResultCard(manager: PlatformMusicManager, track: PlatformMusicInfo) {
        val id = manager.modId(track)
        val exists = id in DataSourceMusic.library
        val busy = id in importing

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shape.v5,
            shadowElevation = Theme.shadow.v7,
            border = BorderStroke(Theme.border.v7, Theme.color.outline.copy(alpha = 0.27f)),
        ) {
            WebImage(
                source = track.pic.ifBlank { null },
                contentScale = ContentScale.Crop,
                alpha = 0.3f,
                modifier = Modifier.matchParentSize(),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    SimpleEllipsisText(text = track.name, style = Theme.typography.v6.bold)
                    SimpleEllipsisText(text = track.singer.ifBlank { "未知歌手" }, color = Theme.color.onSurfaceVariant)
                }

                if (exists) {
                    TextButton(
                        text = "已下载",
                        icon = Icons.Check,
                        enabled = false,
                        onClick = { }
                    )
                }
                else {
                    PrimaryTextButton(
                        text = if (busy) "导入中" else "导入",
                        icon = Icons.Download,
                        enabled = !busy,
                        onClick = { manager.download(id, track) }
                    )
                }
            }
        }
    }

    @Composable
    override fun Content() {
        LaunchedEffect(input.text) {
            if (selection?.input != input.text) selection = null
        }

        Theme.ThemeModeWrapper(true) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().background(Theme.color.background),
                contentPadding = Theme.padding.eValue9,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            ) {
                item(key = ItemKey("Search"), span = { GridItemSpan(maxLineSpan) }) {
                    SearchControls()
                }

                resultManager?.then { manager ->
                    items(items = results, key = { "${manager.type.prefix}${it.id}" }) {
                        ResultCard(manager, it)
                    }
                }
            }
        }
    }
}
