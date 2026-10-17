package love.yinlin.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import love.yinlin.annotation.LooseTyped
import love.yinlin.app
import love.yinlin.app.world.resources.Res
import love.yinlin.app.world.resources.card_concert_memory
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.Text
import love.yinlin.concurrent.Mutex
import love.yinlin.cs.ApiConcertGetConcerts
import love.yinlin.cs.ApiConcertSetAttended
import love.yinlin.cs.ServerRes
import love.yinlin.cs.UnauthorizedException
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.concert.ConcertItem
import love.yinlin.data.rachel.concert.ConcertMemoryState
import love.yinlin.data.rachel.concert.ConcertSong
import love.yinlin.data.rachel.concert.ConcertSongItem
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

@Stable
class ScreenConcert : Screen() {
    private var memory by mutableRefStateOf(ConcertMemoryState.Empty)
    private var pendingCid: Int? by mutableStateOf(null)

    private val requestMutex = Mutex()
    private val pagerState = PagerState { 3 }
    private val concertState = LazyGridState()
    private val songState = LazyListState()
    private val footprintsConcertState = LazyGridState()
    private val footprintsSongState = LazyGridState()
    private val provider = RachelStatefulProvider(
        initStatus = StatefulStatus.Loading,
        emptyText = "演出与歌单正在整理中",
        loadingText = "正在整理现场记忆...",
        networkErrorHandler = { launch { requestData() } },
        emptyHandler = { launch { requestData() } },
        emptyHandlerText = "刷新",
    )

    private suspend fun requestData() {
        requestMutex.with {
            val token = app.config.userToken
            if (token.isEmpty()) {
                provider.status = StatefulStatus.Custom
                return@with
            }

            provider.status = StatefulStatus.Loading

            val error = ApiConcertGetConcerts.request(token) { concerts, songs, attended ->
                if (token == app.config.userToken) memory = ConcertMemoryState.create(concerts, songs, attended)
            }

            if (token != app.config.userToken) return@with

            provider.status = when {
                error is UnauthorizedException -> StatefulStatus.Custom
                error != null -> StatefulStatus.NetworkError
                memory.concerts.isEmpty() && memory.songs.isEmpty() -> StatefulStatus.Empty
                else -> StatefulStatus.Content
            }
        }
    }

    private fun toggleAttended(item: ConcertItem) {
        if (pendingCid != null || provider.status != StatefulStatus.Content) return
        val token = app.config.userToken
        if (token.isEmpty()) return
        pendingCid = item.concert.cid
        launch {
            cleaning({ pendingCid = null }) {
                requestMutex.with {
                    if (token != app.config.userToken) return@with
                    ApiConcertSetAttended.request(token, item.concert.cid, !item.attended) { attended ->
                        if (token == app.config.userToken) memory = memory.withAttended(attended)
                    }.errorTip
                }
            }
        }
    }

    private fun openConcert(item: ConcertItem) {
        launch {
            if (!concertDialog.isOpen) concertDialog.open(item.concert.cid)
        }
    }

    @OptIn(LooseTyped::class)
    private fun openSong(song: ConcertSong) {
        val pid = song.pid?.takeIf { it.isNotBlank() } ?: return
        concertDialog.close()
        navigate("ScreenMusicDetails", pid)
    }

    override val title: String = "演出记忆"

    override fun initialize() {
        launch { requestData() }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Refresh, tip = "刷新演出记忆", enabled = !provider.isLoading && pendingCid == null) {
            requestData()
        }
    }

    @Composable
    private fun ConcertSectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
            Text(text = title, style = Theme.typography.v5.bold)
            Text(text = subtitle, style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
        }
    }

    @Composable
    private fun ConcertMessage(title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v4),
        ) {
            Icon(icon = Icons.LibraryMusic, modifier = Modifier.size(Theme.size.image8), color = Theme.color.primary)
            Text(text = title, style = Theme.typography.v5.bold)
            Text(text = description, style = Theme.typography.v7, color = Theme.color.onSurfaceVariant)
            if (action != null) PrimaryButton(text = action, onClick = onAction)
        }
    }

    @Composable
    private fun ConcertBadge(text: String, color: Color, background: Color) {
        Text(
            text = text,
            color = color,
            style = Theme.typography.v8.bold,
            modifier = Modifier.background(background, Theme.shape.circle).padding(Theme.padding.value),
        )
    }

    @Composable
    private fun ConcertAttendanceButton(item: ConcertItem, pendingCid: Int?, onToggle: (ConcertItem) -> Unit) {
        val saving = pendingCid == item.concert.cid
        val enabled = pendingCid == null
        val heartColor by animateColorAsState(
            targetValue = (if (item.attended) Colors.Red4 else Colors.White).copy(alpha = if (enabled || saving) 1f else 0.5f),
            animationSpec = tween(Theme.animation.duration.v9),
        )
        Box(
            modifier = Modifier.size(Theme.size.image8).clip(Theme.shape.circle)
                .background(Colors.Black.copy(alpha = 0.5f))
                .toggleable(value = item.attended, enabled = enabled, onValueChange = { onToggle(item) })
                .pointerIcon(PointerIcon.Hand, enabled = enabled),
            contentAlignment = Alignment.Center,
        ) {
            if (saving) CircleLoading.Content(color = Colors.White, modifier = Modifier.size(Theme.size.icon))
            else Icon(icon = if (item.attended) Icons.Favorite else Icons.FavoriteBorder, color = heartColor)
        }
    }

    @Composable
    private fun ConcertSongCover(song: ConcertSong, compact: Boolean = false) {
        val cover = remember(song.pid) {
            song.pid?.takeIf { it.isNotBlank() }?.let { ServerRes.Mod.Song(it).res(ModResourceType.Record.filename).url } ?: ""
        }
        WebImage(
            uri = cover,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(if (compact) Theme.size.image9 else Theme.size.image8).clip(Theme.shape.v7).background(Colors.Black),
        )
    }

    @Composable
    private fun ConcertSetlistRow(song: ConcertSong, sequence: Int, onOpenSong: (ConcertSong) -> Unit) {
        val linked = !song.pid.isNullOrBlank()
        Surface(
            modifier = Modifier.fillMaxWidth().pointerIcon(PointerIcon.Hand, enabled = linked),
            shape = Theme.shape.v7,
            tonalLevel = 1,
            onClick = if (linked) ({ onOpenSong(song) }) else null,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConcertSongCover(song = song, compact = true)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.g7)) {
                    Text(text = sequence.toString().padStart(2, '0'), style = Theme.typography.v9, color = Theme.color.onSurfaceVariant)
                    Text(text = song.name, style = Theme.typography.v8.bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun ConcertCard(
        item: ConcertItem,
        pendingCid: Int?,
        onToggle: (ConcertItem) -> Unit,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    ) {
        val borderColor by animateColorAsState(
            targetValue = if (item.attended) Theme.color.primary else Theme.color.outline.copy(alpha = 0.25f),
            animationSpec = tween(Theme.animation.duration.v9),
        )
        Surface(
            modifier = modifier.pointerIcon(PointerIcon.Hand, enabled = onClick != null),
            shape = Theme.shape.v3,
            border = BorderStroke(Theme.border.v7, borderColor),
            shadowElevation = Theme.shadow.v8,
            onClick = onClick,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val poster = item.posterRes
                    if (poster == null) {
                        Image(res = Res.drawable.card_concert_memory, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    }
                    else WebImage(uri = poster.url, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(
                        colors = [Colors.Black.copy(alpha = 0.35f), Colors.Black.copy(alpha = 0.85f)],
                    )))
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = Theme.size.image2)
                            .padding(start = Theme.padding.e7, end = Theme.padding.e7, top = Theme.padding.v, bottom = Theme.padding.v7),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "LIVE · ${item.songs.size} 首", color = Colors.White, style = Theme.typography.v7.bold)
                            ConcertAttendanceButton(item = item, pendingCid = pendingCid, onToggle = onToggle)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)) {
                            Text(text = item.concert.title, color = Colors.White, style = Theme.typography.v5.bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = Theme.size.input8),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(icon = Icons.EventAvailable, color = Colors.White, modifier = Modifier.size(Theme.size.smallIcon))
                                Text(text = item.concert.date, color = Colors.White, style = Theme.typography.v7, maxLines = 1)
                                val location = item.concert.location?.takeIf { it.isNotBlank() }
                                if (location != null) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(icon = Icons.AddLocation, color = Colors.White, modifier = Modifier.size(Theme.size.smallIcon))
                                        Text(text = location, color = Colors.White, style = Theme.typography.v7, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                else Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertSongRow(item: ConcertSongItem, onOpenSong: (ConcertSong) -> Unit, personal: Boolean = false) {
        val pid = item.song.pid?.takeIf { it.isNotBlank() }
        val background = remember(pid) { pid?.let { ServerRes.Mod.Song(it).res(ModResourceType.Background.filename).url } }
        Surface(
            modifier = Modifier.fillMaxWidth().pointerIcon(PointerIcon.Hand, enabled = pid != null),
            shape = Theme.shape.v7,
            tonalLevel = 1,
            onClick = if (pid != null) ({ onOpenSong(item.song) }) else null,
        ) {
            if (background != null) WebImage(uri = background, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop, alpha = 0.12f)
            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConcertSongCover(song = item.song)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    Text(text = item.song.name, style = Theme.typography.v7.bold)
                    Text(
                        text = if (item.heardCount > 0) "在 ${item.heardCount} 场现场听过" else "还未在现场听过",
                        style = Theme.typography.v8,
                        color = if (item.heardCount > 0) Theme.color.primary else Theme.color.onSurfaceVariant,
                    )
                }
                ConcertBadge(
                    text = "${if (personal) item.heardCount else item.concertCount} 场",
                    color = Theme.color.primary,
                    background = Theme.color.primary.copy(alpha = 0.1f),
                )
                if (pid != null) Icon(icon = Icons.KeyboardArrowRight, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
            }
        }
    }



    @Composable
    private fun ConcertTabs(state: PagerState, onNavigate: (Int) -> Unit) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e, vertical = Theme.padding.v),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier.widthIn(max = Theme.size.cell1 * 2).fillMaxWidth()
                    .clip(Theme.shape.circle)
                    .background(Theme.color.primary.copy(alpha = 0.08f))
                    .border(Theme.border.v7, Theme.color.outline.copy(alpha = 0.15f), Theme.shape.circle)
                    .padding(Theme.padding.g7),
            ) {
                val tabWidth = maxWidth / 3
                val showIcons = tabWidth >= (Theme.size.input5 + Theme.padding.h) * maxOf(1f, LocalDensity.current.fontScale)
                // 底块跟随 Pager 的实际偏移，同时响应手势滑动与点击切页。
                Box(modifier = Modifier.matchParentSize()) {
                    Surface(
                        modifier = Modifier.width(tabWidth).fillMaxHeight().offset {
                            val position = (state.currentPage + state.currentPageOffsetFraction).coerceIn(0f, 2f)
                            IntOffset(x = (tabWidth.toPx() * position).roundToInt(), y = 0)
                        },
                        shape = Theme.shape.circle,
                        tonalLevel = 1,
                        shadowElevation = Theme.shadow.v8,
                        border = BorderStroke(Theme.border.v7, Theme.color.outline.copy(alpha = 0.1f)),
                    ) {}
                }
                Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    repeat(3) { index ->
                        val selected = state.currentPage == index
                        val color by animateColorAsState(
                            targetValue = if (selected) Theme.color.primary else Theme.color.onSurfaceVariant,
                            animationSpec = tween(Theme.animation.duration.v9),
                        )
                        Row(
                            modifier = Modifier.weight(1f).clip(Theme.shape.circle)
                                .selectable(selected = selected, onClick = { if (!selected || state.isScrollInProgress) onNavigate(index) })
                                .pointerIcon(PointerIcon.Hand)
                                .heightIn(min = Theme.size.input8).padding(Theme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showIcons) Icon(
                                icon = when (index) {
                                    1 -> Icons.LibraryMusic
                                    2 -> Icons.History
                                    else -> Icons.EventAvailable
                                },
                                color = color,
                                modifier = Modifier.size(Theme.size.smallIcon),
                            )
                            Text(
                                text = when (index) {
                                    1 -> "歌曲"
                                    2 -> "足迹"
                                    else -> "Live"
                                },
                                color = color,
                                style = Theme.typography.v7.bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertDetailsContent(item: ConcertItem, pendingCid: Int?, onToggle: (ConcertItem) -> Unit, onOpenSong: (ConcertSong) -> Unit) {
        val deviceType by rememberDeviceType()
        BoxWithConstraints(
            modifier = (if (deviceType == Device.Type.PORTRAIT) Modifier.fillMaxWidth() else Modifier.width(Theme.size.cell1))
                .heightIn(max = Theme.size.cell1 * 2.5f).fillMaxHeight(0.9f),
        ) {
            val columns = if (maxWidth >= Theme.size.cell6 * 2 * maxOf(1f, LocalDensity.current.fontScale) + Theme.padding.e * 2 + Theme.padding.v) 2 else 1
            Column(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = Theme.padding.eValue,
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                ) {
                    item(key = "concert", span = { GridItemSpan(maxLineSpan) }) {
                        ConcertCard(item = item, pendingCid = pendingCid, onToggle = onToggle, modifier = Modifier.fillMaxWidth())
                    }
                    item(key = "setlist-title", span = { GridItemSpan(maxLineSpan) }) {
                        Text(text = "现场歌单 · ${item.songs.size} 首", style = Theme.typography.v8.bold, modifier = Modifier.padding(Theme.padding.value))
                    }
                    if (item.songs.isEmpty()) {
                        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                            Text(text = "现场歌单还在整理中", style = Theme.typography.v8, modifier = Modifier.padding(Theme.padding.value))
                        }
                    }
                    items(count = item.songs.size, key = { it }, contentType = { "song" }) { index ->
                        ConcertSetlistRow(song = item.songs[index], sequence = index + 1, onOpenSong = onOpenSong)
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertListPage(memory: ConcertMemoryState, state: LazyGridState, pendingCid: Int?, onToggle: (ConcertItem) -> Unit, onOpenConcert: (ConcertItem) -> Unit) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Theme.size.cell1 * maxOf(1f, LocalDensity.current.fontScale)),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Theme.padding.e7,
                top = Theme.padding.v,
                end = Theme.padding.e7,
                bottom = Theme.padding.e7,
            ),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.e7),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7),
        ) {
            item(key = "overview", span = { GridItemSpan(maxLineSpan) }) {
                ConcertSectionTitle(title = "演出现场", subtitle = "${memory.concerts.size} 场演出 · 已点亮 ${memory.attendedConcerts.size} 场")
            }
            if (memory.concerts.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ConcertMessage(title = "演出现场正在整理中", description = "新的演出和歌单会在这里与你见面。")
                }
            }
            items(items = memory.concerts, key = { it.concert.cid }, contentType = { "concert" }) { item ->
                ConcertCard(item = item, pendingCid = pendingCid, onToggle = onToggle, modifier = Modifier.fillMaxWidth(), onClick = { onOpenConcert(item) })
            }
        }
    }

    @Composable
    private fun ConcertSongFilter(selected: Int, onSelect: (Int) -> Unit) {
        Filter(
            size = 4,
            selectedProvider = { it == selected },
            titleProvider = {
                when (it) {
                    1 -> "听过"
                    2 -> "未听过"
                    3 -> "唯一"
                    else -> "全部"
                }
            },
            style = Theme.typography.v8,
            activeIcon = null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e7, vertical = Theme.padding.v),
            onClick = { index, _ -> if (index != selected) onSelect(index) },
        )
    }

    @Composable
    private fun ConcertSongsPage(memory: ConcertMemoryState, state: LazyListState, onOpenSong: (ConcertSong) -> Unit) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        val songs = when (selected) {
            1 -> memory.heardSongs
            2 -> memory.unheardSongs
            3 -> memory.uniqueSongs
            else -> memory.songs
        }
        Column(modifier = Modifier.fillMaxSize()) {
            ConcertSongFilter(selected = selected, onSelect = {
                selected = it
                state.requestScrollToItem(0)
            })
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = state,
                contentPadding = PaddingValues(
                    start = Theme.padding.e7,
                    top = Theme.padding.v,
                    end = Theme.padding.e7,
                    bottom = Theme.padding.e7,
                ),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
            ) {
                if (songs.isEmpty()) {
                    item(key = "empty") {
                        ConcertMessage(
                            title = if (memory.songs.isEmpty()) "现场歌单正在整理中" else when (selected) {
                                1 -> "还没有听过的现场歌曲"
                                2 -> "所有歌曲都在你的记忆里了"
                                3 -> "暂时没有只出现一次的歌曲"
                                else -> "现场歌单正在整理中"
                            },
                            description = when (selected) {
                                1 -> "点亮一场看过的演出，就能收集那一晚的歌。"
                                3 -> "这里收录只在一场演出中出现过的歌曲。"
                                else -> "每一首歌，都可能在下一场与你相遇。"
                            },
                        )
                    }
                }
                items(items = songs, key = { it.song.sid }, contentType = { "song" }) { item ->
                    ConcertSongRow(item = item, onOpenSong = onOpenSong)
                }
            }
        }
    }

    @Composable
    private fun ConcertFootprintsTabs(selected: Int, onSelect: (Int) -> Unit) {
        val position by animateFloatAsState(
            targetValue = selected.toFloat(),
            animationSpec = tween(Theme.animation.duration.v9),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().background(Theme.color.primary.copy(alpha = 0.04f))) {
            val tabWidth = maxWidth / 2
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier.width(tabWidth).fillMaxHeight()
                        .offset { IntOffset(x = (tabWidth.toPx() * position).roundToInt(), y = 0) }
                        .background(Theme.color.primary.copy(alpha = 0.1f)),
                ) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(Theme.border.v3).background(Theme.color.primary))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                repeat(2) { index ->
                    val isSelected = selected == index
                    val color by animateColorAsState(
                        targetValue = if (isSelected) Theme.color.primary else Theme.color.onSurfaceVariant,
                        animationSpec = tween(Theme.animation.duration.v9),
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                            .selectable(selected = isSelected, onClick = { if (!isSelected) onSelect(index) })
                            .pointerIcon(PointerIcon.Hand)
                            .heightIn(min = Theme.size.input8).padding(Theme.padding.value),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (index == 0) "演出" else "歌曲",
                            color = color,
                            style = Theme.typography.v7.bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertFootprintsSummary(
        memory: ConcertMemoryState,
        selected: Int,
        onSelect: (Int) -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shape.v3,
            tonalLevel = 3
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue7),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v4)
                ) {
                    ConcertSectionTitle(title = "我的现场记忆", subtitle = "把去过的现场，留在自己的歌单里")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                Text(text = memory.attendedConcerts.size.toString(), style = Theme.typography.v3.bold, color = Theme.color.primary, modifier = Modifier.alignByBaseline())
                                Text(text = "/ ${memory.concerts.size}", style = Theme.typography.v7, color = Theme.color.onSurfaceVariant, modifier = Modifier.alignByBaseline())
                            }
                            Text(text = "场演出看过", style = Theme.typography.v8, textAlign = TextAlign.Center)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                Text(text = memory.heardSongs.size.toString(), style = Theme.typography.v3.bold, color = Theme.color.primary, modifier = Modifier.alignByBaseline())
                                Text(text = "/ ${memory.songs.size}", style = Theme.typography.v7, color = Theme.color.onSurfaceVariant, modifier = Modifier.alignByBaseline())
                            }
                            Text(text = "首不同的歌听过", style = Theme.typography.v8, textAlign = TextAlign.Center)
                        }
                    }
                }
                ConcertFootprintsTabs(selected = selected, onSelect = onSelect)
            }
        }
    }

    @Composable
    private fun ConcertFootprintsPage(
        memory: ConcertMemoryState,
        concertState: LazyGridState,
        songState: LazyGridState,
        pendingCid: Int?,
        onToggle: (ConcertItem) -> Unit,
        onOpenConcert: (ConcertItem) -> Unit,
        onOpenSong: (ConcertSong) -> Unit,
        onExplore: () -> Unit,
    ) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Theme.size.cell1 * maxOf(1f, LocalDensity.current.fontScale)),
            state = if (selected == 0) concertState else songState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Theme.padding.e7,
                top = Theme.padding.v,
                end = Theme.padding.e7,
                bottom = Theme.padding.e7,
            ),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7),
            verticalArrangement = Arrangement.spacedBy(if (selected == 0) Theme.padding.e7 else Theme.padding.v7),
        ) {
            item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                ConcertFootprintsSummary(memory = memory, selected = selected, onSelect = { selected = it })
            }
            if (selected == 0 && memory.attendedConcerts.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ConcertMessage(
                        title = "你的第一场现场，值得记住",
                        description = "点亮看过的演出，它的歌单就会成为你的足迹。",
                        action = "去点亮现场",
                        onAction = onExplore,
                    )
                }
            }
            else if (selected == 0) {
                items(items = memory.attendedConcerts, key = { "concert-${it.concert.cid}" }, contentType = { "concert" }) { item ->
                    ConcertCard(item = item, pendingCid = pendingCid, onToggle = onToggle, modifier = Modifier.fillMaxWidth(), onClick = { onOpenConcert(item) })
                }
            }
            else if (memory.heardSongs.isEmpty()) {
                item(key = "songs-empty", span = { GridItemSpan(maxLineSpan) }) {
                    ConcertMessage(
                        title = if (memory.attendedConcerts.isEmpty()) "还没有听过的现场歌曲" else "现场歌单正在整理中",
                        description = if (memory.attendedConcerts.isEmpty()) "点亮看过的演出，就能收集它的现场歌曲。" else "这些现场的歌单补齐后，会自动计入你的记忆。",
                        action = if (memory.attendedConcerts.isEmpty()) "去点亮现场" else null,
                        onAction = onExplore,
                    )
                }
            }
            else {
                items(items = memory.heardSongs, key = { "song-${it.song.sid}" }, span = { GridItemSpan(maxLineSpan) }, contentType = { "song" }) { item ->
                    ConcertSongRow(item = item, onOpenSong = onOpenSong, personal = true)
                }
            }
        }
    }

    @Composable
    override fun Content() {
        // Pager 动画需要 Composition 提供的 FrameClock，不能使用 Screen.launch。
        val animationScope = rememberCoroutineScope()
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            ConcertTabs(state = pagerState, onNavigate = { animationScope.launch { pagerState.animateScrollToPage(it) } })
            if (provider.status == StatefulStatus.Custom) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    ConcertMessage(
                        title = "登录后，收藏你的现场记忆",
                        description = "请在「我的」中登录账号，演出足迹会随账号保存。",
                        action = "返回",
                        onAction = ::onBack,
                    )
                }
            }
            else {
                StatefulBox(provider = provider, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
                        when (page) {
                            0 -> ConcertListPage(memory = memory, state = concertState, pendingCid = pendingCid, onToggle = ::toggleAttended, onOpenConcert = ::openConcert)
                            1 -> ConcertSongsPage(memory = memory, state = songState, onOpenSong = ::openSong)
                            2 -> ConcertFootprintsPage(
                                memory = memory,
                                concertState = footprintsConcertState,
                                songState = footprintsSongState,
                                pendingCid = pendingCid,
                                onToggle = ::toggleAttended,
                                onOpenConcert = ::openConcert,
                                onOpenSong = ::openSong,
                                onExplore = { animationScope.launch { pagerState.animateScrollToPage(0) } },
                            )
                        }
                    }
                }
            }
        }
    }

    private val concertDialog = this land object : Dialog<Unit>() {
        private var cid by mutableIntStateOf(0)

        suspend fun open(cid: Int) {
            this.cid = cid
            awaitResult()
        }

        @Composable
        override fun Land() {
            LandDialog {
                val item = memory.concerts.firstOrNull { it.concert.cid == cid }
                if (item != null) {
                    key(cid) {
                        ConcertDetailsContent(
                            item = item,
                            pendingCid = pendingCid,
                            onToggle = ::toggleAttended,
                            onOpenSong = ::openSong
                        )
                    }
                }
                else ConcertMessage(title = "演出资料暂不可用", description = "关闭后刷新演出列表再试。")
            }
        }
    }
}
