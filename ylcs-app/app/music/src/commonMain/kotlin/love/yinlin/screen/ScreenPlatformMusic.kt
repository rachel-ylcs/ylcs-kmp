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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.Job
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.PrimaryOutlinedButton
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.catchingError
import love.yinlin.screen.PlatformMusicModImporter.importTrack
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.music.PlatformMusicAPI
import love.yinlin.tpl.music.PlatformMusicResolvers
import love.yinlin.uri.Uri

@Stable
class ScreenPlatformMusic(deeplink: Uri? = null, platformType: PlatformMusicType? = null) : Screen() {
    override val title: String = "平台音乐"

    private val input = InputState(deeplink?.toString().orEmpty(), maxLength = 4096, imeAction = ImeAction.Search)
    private var lastInput = input.text
    private var selectedPlatform by mutableStateOf(platformType ?: PlatformMusicResolvers.identify(lastInput)?.type)
    private var manuallySelected by mutableStateOf(platformType != null)
    private var selectionHint by mutableStateOf<String?>(null)

    private val tracks = mutableStateListOf<PlatformMusicInfo>()
    private var resultResolver by mutableStateOf<PlatformMusicAPI?>(null)
    private var searching by mutableStateOf(false)
    private var resultMessage by mutableStateOf<String?>(null)
    private var searchJob: Job? = null
    private val imports = mutableStateMapOf<String, Unit>()
    private val gridState = LazyGridState()

    override fun initialize() {
        launch { snapshotFlow { input.text }.collect { syncInput() } }
    }

    private fun syncInput() {
        val value = input.text
        if (value == lastInput) return
        lastInput = value
        selectedPlatform = PlatformMusicResolvers.identify(value)?.type
        manuallySelected = false
        selectionHint = null
    }

    private fun selectPlatform(type: PlatformMusicType) {
        syncInput()
        selectedPlatform = type.takeUnless { it == selectedPlatform }
        manuallySelected = selectedPlatform != null
        selectionHint = null
    }

    private fun search() {
        syncInput()
        val value = lastInput.trim()
        if (value.isEmpty() || searching) return
        val resolver = selectedPlatform?.let { PlatformMusicResolvers[it] } ?: PlatformMusicResolvers.identify(value)
        if (resolver == null) {
            selectionHint = if (PlatformMusicResolvers.links(value).any()) "这个链接未匹配到支持的平台" else "搜索关键词前，请选择音乐平台"
            return
        }

        val links = PlatformMusicResolvers.links(value).toList()
        val link = links.firstOrNull(resolver::matchesLink)
        if (link == null && links.isNotEmpty()) {
            selectionHint = "当前链接与${resolver.type.description}不匹配，请切换平台"
            return
        }

        selectedPlatform = resolver.type
        selectionHint = null
        tracks.clear()
        gridState.requestScrollToItem(0)
        resultResolver = resolver
        resultMessage = null
        searching = true
        searchJob = launch {
            val seen = mutableSetOf<String>()
            try {
                val failure = catchingError {
                    val query = when {
                        link != null -> resolver.parseLink(link)
                        resolver.matchesTrackId(value) -> resolver.findTrack(value)
                        else -> resolver.search(value)
                    }
                    query.collect { batch -> tracks.addAll(batch.filter { seen.add(it.id) }) }
                }
                resultMessage = failure?.message ?: if (tracks.isEmpty()) "未找到歌曲，请试试其他关键词或链接" else null
            }
            finally {
                searching = false
            }
        }
    }

    private fun stopSearch() {
        searchJob?.cancel()
        resultMessage = "已停止检索"
    }

    private fun download(resolver: PlatformMusicAPI, track: PlatformMusicInfo) {
        val player = app.requireClassOrNull<StartupMusicPlayer>() ?: run {
            slot.tip.warning("播放器尚未就绪")
            return
        }
        val id = resolver.modId(track)
        if (imports.put(id, Unit) != null) return
        launch {
            try {
                val failure = catchingError { importTrack(player, resolver, track, id) }
                if (failure == null) slot.tip.success("已将「${track.name}」导入曲库")
                else slot.tip.warning(failure.message ?: "导入失败，请重试")
            }
            finally {
                imports.remove(id)
            }
        }
    }

    @Composable
    private fun PlatformCard(type: PlatformMusicType, modifier: Modifier = Modifier) {
        val selected = selectedPlatform == type
        val icon = when (type) {
            PlatformMusicType.QQMusic -> Icons2.QQMusic
            PlatformMusicType.NetEaseCloud -> Icons2.NetEaseCloudMusic
            PlatformMusicType.Kugou -> Icons2.KugouMusic
            PlatformMusicType.Soda -> Icons2.SodaMusic
            PlatformMusicType.Migu -> Icons2.MiguMusic
        }
        val shape = Theme.shape.v7
        val color = if (selected) Theme.color.primary else Theme.color.onSurface
        Row(
            modifier = modifier
                .heightIn(min = Theme.size.image8)
                .clip(shape)
                .background(if (selected) Theme.color.primary.copy(alpha = 0.11f) else Theme.color.surface.copy(alpha = 0.76f))
                .border(Theme.border.v7, if (selected) Theme.color.primary else Theme.color.outline.copy(alpha = 0.28f), shape)
                .semantics { stateDescription = if (selected) "已选择，再按一次取消" else "未选择" }
                .selectable(
                    selected = selected,
                    role = Role.Button,
                    onClick = { selectPlatform(type) }
                )
                .padding(horizontal = Theme.padding.h, vertical = Theme.padding.v9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon, color = Colors.Unspecified, modifier = Modifier.size(Theme.size.icon))
            SimpleEllipsisText(text = type.description, color = color, style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))
            if (selected) Icon(icon = Icons.Check, color = color, modifier = Modifier.size(Theme.size.smallIcon))
        }
    }

    @Composable
    private fun SearchPanel() {
        val enabled = input.text.isNotBlank()
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < Theme.size.sheet * 2
            val inset = if (compact) Theme.padding.value7 else Theme.padding.value4
            val colors = Theme.color
            val shape = Theme.shape.v3
            Column(
                modifier = Modifier.fillMaxWidth().clip(shape)
                    .background(Brush.linearGradient([colors.primary.copy(alpha = 0.13f), colors.surface, colors.tertiary.copy(alpha = 0.09f)]))
                    .border(Theme.border.v7, colors.outline.copy(alpha = 0.25f), shape)
                    .padding(inset),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v4)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7), verticalAlignment = Alignment.CenterVertically) {
                    if (!compact) {
                        Box(
                            modifier = Modifier.size(Theme.size.image7).clip(Theme.shape.v5).background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon = Icons.LibraryMusic, color = colors.onContainer, modifier = Modifier.size(Theme.size.image9))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                        Text("搜索与导入", style = (if (compact) Theme.typography.v4 else Theme.typography.v3).bold)
                        Text("粘贴音乐分享链接，或按歌名、歌手搜索。", color = colors.onBackgroundVariant, style = Theme.typography.v7)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clip(Theme.shape.v6).background(colors.surface)
                        .border(Theme.border.v7, colors.outline.copy(alpha = 0.45f), Theme.shape.v6)
                        .padding(horizontal = Theme.padding.h, vertical = Theme.padding.v9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!compact) Icon(icon = Icons.Search, color = colors.onSurfaceVariant)
                    Input(
                        state = input,
                        hint = if (compact) "歌曲、歌手或链接" else "歌曲、歌手、歌曲 ID 或分享链接",
                        trailing = InputDecoration.Icon.Clear,
                        onImeClick = {
                            search()
                            true
                        },
                        modifier = Modifier.weight(1f).semantics { contentDescription = "音乐搜索" }
                    )
                    if (searching) PrimaryOutlinedButton(text = "停止", icon = Icons.Cancel, onClick = ::stopSearch)
                    else PrimaryButton(text = "搜索", enabled = enabled, onClick = ::search)
                }

                Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
                        Text("音乐平台", style = Theme.typography.v7.bold)
                        Text(
                            when {
                                manuallySelected -> "手动选择 · 再点取消"
                                selectedPlatform != null -> "已自动识别"
                                else -> "自动识别链接"
                            },
                            color = if (selectedPlatform != null) colors.primary else colors.onBackgroundVariant,
                            style = Theme.typography.v8
                        )
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val gap = Theme.padding.h
                        val columns = when {
                            maxWidth >= Theme.size.cell4 * 5 + gap * 4 -> 5
                            maxWidth >= Theme.size.cell4 * 3 + gap * 2 -> 3
                            maxWidth >= Theme.size.cell5 * 2 + gap -> 2
                            else -> 1
                        }
                        val width = (maxWidth - gap * (columns - 1)) / columns
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
                            maxItemsInEachRow = columns
                        ) {
                            for (platform in PlatformMusicResolvers.all) PlatformCard(platform.type, Modifier.width(width))
                        }
                    }
                    selectionHint?.let { Text(it, color = colors.warning, style = Theme.typography.v7) }
                }
            }
        }
    }

    @Composable
    private fun ResultsHeader() {
        val resolver = resultResolver
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText("搜索结果", style = Theme.typography.v5.bold, modifier = Modifier.weight(1f))
            if (searching) CircleLoading.Content(modifier = Modifier.size(Theme.size.smallIcon), color = Theme.color.primary)
            if (resolver != null) {
                SimpleEllipsisText(
                    text = "${resolver.type.description} · ${tracks.size} 首",
                    color = Theme.color.onSurfaceVariant,
                    style = Theme.typography.v7
                )
            }
        }
    }

    @Composable
    private fun ResultCard(resolver: PlatformMusicAPI, track: PlatformMusicInfo) {
        val id = resolver.modId(track)
        val imported = id in DataSourceMusic.library
        val importing = id in imports
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shape.v6,
            border = BorderStroke(Theme.border.v7, Theme.color.outline.copy(alpha = 0.27f)),
            contentPadding = Theme.padding.value8
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val size = Theme.size.image8
                Box(
                    modifier = Modifier.size(size).clip(Theme.shape.v8).background(Theme.color.backgroundVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon = Icons.MusicNote, color = Theme.color.primary)
                    if (track.pic.isNotBlank()) WebImage(source = track.pic, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    SimpleEllipsisText(track.name, style = Theme.typography.v6.bold)
                    SimpleEllipsisText(track.singer.ifBlank { "未知歌手" }, color = Theme.color.onSurfaceVariant, style = Theme.typography.v8)
                }
                PrimaryTextButton(
                    text = when {
                        importing -> "导入中"
                        imported -> "重新导入"
                        else -> "导入"
                    },
                    icon = if (imported && !importing) Icons.Check else Icons.Download,
                    enabled = !importing,
                    onClick = { download(resolver, track) }
                )
            }
        }
    }

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.sheet),
                state = gridState,
                modifier = Modifier.widthIn(max = Theme.size.sheet * 3).fillMaxWidth().fillMaxHeight(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { SearchPanel() }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.padding(top = Theme.padding.v5)) { ResultsHeader() }
                }
                val resolver = resultResolver
                if (resolver != null) {
                    items(tracks, key = { "${resolver.type.prefix}${it.id}" }) { ResultCard(resolver, it) }
                }
                val message = resultMessage ?: when {
                    searching && tracks.isEmpty() -> "正在查找歌曲…"
                    resolver == null -> "找到想要的歌曲后，可直接导入曲库。"
                    else -> null
                }
                if (message != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = Theme.padding.value5,
                            shape = Theme.shape.v6,
                            tonalLevel = 1
                        ) {
                            Text(message, color = Theme.color.onSurfaceVariant, style = Theme.typography.v7)
                        }
                    }
                }
            }
        }
    }
}
