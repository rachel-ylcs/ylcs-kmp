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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
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
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.concert.Concert
import love.yinlin.data.rachel.concert.ConcertSong
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

@Stable
class ScreenConcert : BasicScreen() {
    @Stable
    private data class ConcertItem(
        val concert: Concert,
        val songs: List<ConcertSong>,
        val attended: Boolean = false,
    ) {
        val posterRes: APIRes? = concert.poster?.let { poster ->
            val parts = poster.split('/', limit = 2)
            val classification = parts[0].takeIf { it.isNotBlank() && it != "." && it != ".." } ?: return@let null
            val index = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0 .. Int.MAX_VALUE } ?: return@let null
            ServerRes.Photo.pic(classification = classification, index = index, thumb = true)
        }
    }

    @Stable
    private data class ConcertSongItem(
        val song: ConcertSong,
        val concertCount: Int,
        val heardCount: Int = 0,
    )

    @Stable
    private class ConcertMemoryState private constructor(
        val concerts: List<ConcertItem>,
        val songs: List<ConcertSongItem>,
    ) {
        val uniqueSongs: List<ConcertSongItem> = songs.filter { it.concertCount == 1 }
        val attendedConcerts: List<ConcertItem> = concerts.filter { it.attended }
        val heardSongs: List<ConcertSongItem> = songs.filter { it.heardCount > 0 }
        val unheardSongs: List<ConcertSongItem> = songs.filter { it.heardCount == 0 }

        fun withAttended(attended: List<Int>): ConcertMemoryState {
            val attendedIds = attended.toSet()
            val updatedConcerts = concerts.map { it.copy(attended = it.concert.cid in attendedIds) }
            val heardCounts = mutableMapOf<Int, Int>()

            for (item in updatedConcerts) {
                if (item.attended) {
                    for (song in item.songs.distinctBy { it.sid }) {
                        heardCounts[song.sid] = (heardCounts[song.sid] ?: 0) + 1
                    }
                }
            }

            return ConcertMemoryState(
                concerts = updatedConcerts,
                songs = songs.map { it.copy(heardCount = heardCounts[it.song.sid] ?: 0) },
            )
        }

        companion object {
            val Empty = ConcertMemoryState([], [])

            fun create(concerts: List<Concert>, songs: List<ConcertSong>, attended: List<Int>): ConcertMemoryState {
                val songCatalog = songs.distinctBy { it.sid }.sortedBy { it.sid }
                val songById = songCatalog.associateBy { it.sid }
                val concertItems = concerts.distinctBy { it.cid }
                    .sortedWith(compareByDescending<Concert> { it.date }.thenByDescending { it.cid })
                    .map { ConcertItem(concert = it, songs = it.songs.mapNotNull(songById::get)) }

                val concertCounts = mutableMapOf<Int, Int>()
                for (item in concertItems) {
                    for (song in item.songs.distinctBy { it.sid }) {
                        concertCounts[song.sid] = (concertCounts[song.sid] ?: 0) + 1
                    }
                }

                val songItems = songCatalog.map { ConcertSongItem(it, concertCounts[it.sid] ?: 0) }

                return ConcertMemoryState(concertItems, songItems).withAttended(attended)
            }
        }
    }

    private var memory by mutableRefStateOf(ConcertMemoryState.Empty)
    private var pendingCid: Int? by mutableStateOf(null)

    private val pagerState = PagerState { 3 }
    private val concertState = LazyGridState()
    private val songState = LazyListState()
    private val footprintsConcertState = LazyGridState()
    private val footprintsSongState = LazyGridState()
    private val provider = RachelStatefulProvider()

    private val concertBrush = Brush.verticalGradient(colors = [Colors.Black.copy(alpha = 0.35f), Colors.Black.copy(alpha = 0.85f)])

    private suspend fun requestData() {
        val token = app.config.userToken

        provider.withLoading {
            val [concerts, songs, attended] = ApiConcertGetConcerts.requestNull(token)!!
            memory = ConcertMemoryState.create(concerts, songs, attended)
            concerts.isNotEmpty() && songs.isNotEmpty()
        }
    }

    private fun toggleAttended(item: ConcertItem) {
        if (pendingCid != null || provider.status != StatefulStatus.Content) return
        val token = app.config.userToken
        if (token.isEmpty()) return
        pendingCid = item.concert.cid
        launch {
            cleaning({
                ApiConcertSetAttended.request(token, item.concert.cid, !item.attended) { attended ->
                    memory = memory.withAttended(attended)
                }.errorTip
            }) {
                pendingCid = null
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

    override fun initialize() {
        launch { requestData() }
    }

    @Composable
    private fun ConcertSectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            SimpleEllipsisText(text = title, style = Theme.typography.v5.bold)
            SimpleEllipsisText(text = subtitle, style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
        }
    }

    @Composable
    private fun ConcertMessage(
        title: String,
        description: String,
        action: String? = null,
        onAction: () -> Unit = {}
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue7),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
        ) {
            Icon(icon = Icons.LibraryMusic, modifier = Modifier.size(Theme.size.image8), color = Theme.color.primary)
            SimpleEllipsisText(text = title, style = Theme.typography.v6.bold)
            SimpleEllipsisText(text = description, style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
            if (action != null) {
                PrimaryButton(icon = Icons.KeyboardArrowRight, text = action, onClick = onAction)
            }
        }
    }

    @Composable
    private fun ConcertAttendanceButton(item: ConcertItem, pendingCid: Int?) {
        val saving = pendingCid == item.concert.cid
        val enabled = pendingCid == null
        val heartColor by animateColorAsState(
            targetValue = (if (item.attended) Colors.Red4 else Colors.White).copy(alpha = if (enabled || saving) 1f else 0.5f),
            animationSpec = tween(Theme.animation.duration.v9),
        )

        Box(
            modifier = Modifier.size(Theme.size.image8)
                .clip(Theme.shape.circle)
                .background(Colors.Black.copy(alpha = 0.5f))
                .toggleable(value = item.attended, enabled = enabled, onValueChange = { toggleAttended(item) })
                .pointerIcon(PointerIcon.Hand, enabled = enabled),
            contentAlignment = Alignment.Center
        ) {
            if (saving) CircleLoading.Content(color = Colors.White, modifier = Modifier.size(Theme.size.icon))
            else Icon(icon = if (item.attended) Icons.Favorite else Icons.FavoriteBorder, color = heartColor)
        }
    }

    @Composable
    private fun ConcertSetlistRow(song: ConcertSong, sequence: Int) {
        val linked = !song.pid.isNullOrBlank()
        Surface(
            modifier = Modifier.fillMaxWidth().pointerIcon(PointerIcon.Hand, enabled = linked),
            shape = Theme.shape.v7,
            tonalLevel = 1,
            onClick = if (linked) ({ openSong(song) }) else null,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cover = remember(song.pid) {
                    song.pid?.takeIf { it.isNotBlank() }?.let { ServerRes.Mod.Song(it).res(ModResourceType.Record.filename).url } ?: ""
                }

                WebImage(
                    uri = cover,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image9).clip(Theme.shape.v7).background(Colors.Black),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.g7)
                ) {
                    SimpleEllipsisText(
                        text = sequence.toString().padStart(2, '0'),
                        style = Theme.typography.v9,
                        color = Theme.color.onSurfaceVariant
                    )
                    Text(
                        text = song.name,
                        style = Theme.typography.v8.bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun ConcertCard(
        item: ConcertItem,
        pendingCid: Int?,
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
            onClick = onClick
        ) {
            val poster = item.posterRes

            if (poster == null) {
                Image(
                    res = Res.drawable.card_concert_memory,
                    modifier = Modifier.matchParentSize().zIndex(1f),
                    contentScale = ContentScale.Crop
                )
            }
            else {
                WebImage(
                    uri = poster.url,
                    modifier = Modifier.matchParentSize().zIndex(1f),
                    contentScale = ContentScale.Crop
                )
            }

            Box(modifier = Modifier.matchParentSize().background(concertBrush).zIndex(2f))

            Column(
                modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(Theme.padding.eValue7).zIndex(3f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SimpleEllipsisText(
                        text = "LIVE · ${item.songs.size} 首",
                        color = Colors.White,
                        style = Theme.typography.v7.bold
                    )
                    ConcertAttendanceButton(item = item, pendingCid = pendingCid)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                ) {
                    SimpleEllipsisText(
                        text = item.concert.title,
                        color = Colors.White,
                        style = Theme.typography.v5.bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.v),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            icon = Icons.EventAvailable,
                            color = Colors.White,
                            modifier = Modifier.size(Theme.size.smallIcon)
                        )
                        SimpleEllipsisText(
                            text = item.concert.date,
                            color = Colors.White,
                            style = Theme.typography.v7
                        )

                        val location = item.concert.location?.takeIf { it.isNotBlank() }

                        if (location != null) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    icon = Icons.AddLocation,
                                    color = Colors.White,
                                    modifier = Modifier.size(Theme.size.smallIcon)
                                )
                                SimpleEllipsisText(
                                    text = location,
                                    color = Colors.White,
                                    style = Theme.typography.v7,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        else Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertSongRow(item: ConcertSongItem, personal: Boolean = false) {
        val pid = item.song.pid?.takeIf { it.isNotBlank() }
        val background = remember(pid) { pid?.let { ServerRes.Mod.Song(it).res(ModResourceType.Background.filename).url } }

        Surface(
            modifier = Modifier.fillMaxWidth().pointerIcon(PointerIcon.Hand, enabled = pid != null),
            shape = Theme.shape.v7,
            tonalLevel = 1,
            onClick = if (pid != null) ({ openSong(item.song) }) else null,
        ) {
            if (background != null) {
                WebImage(
                    uri = background,
                    modifier = Modifier.matchParentSize().zIndex(1f),
                    contentScale = ContentScale.Crop,
                    alpha = 0.12f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value).zIndex(2f),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cover = remember(item.song.pid) {
                    item.song.pid?.takeIf { it.isNotBlank() }?.let { ServerRes.Mod.Song(it).res(ModResourceType.Record.filename).url } ?: ""
                }

                WebImage(
                    uri = cover,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image8).clip(Theme.shape.v7).background(Colors.Black),
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    SimpleEllipsisText(text = item.song.name, style = Theme.typography.v7.bold)
                    SimpleEllipsisText(
                        text = if (item.heardCount > 0) "在 ${item.heardCount} 场现场听过" else "还未在现场听过",
                        style = Theme.typography.v8,
                        color = if (item.heardCount > 0) Theme.color.primary else Theme.color.onSurfaceVariant,
                    )
                }

                SimpleEllipsisText(
                    text = "${if (personal) item.heardCount else item.concertCount} 场",
                    color = Theme.color.primary,
                    style = Theme.typography.v8.bold,
                    modifier = Modifier.background(Theme.color.primary.copy(alpha = 0.1f), Theme.shape.circle).padding(Theme.padding.value),
                )

                if (pid != null) {
                    Icon(
                        icon = Icons.KeyboardArrowRight,
                        color = Theme.color.primary,
                        modifier = Modifier.size(Theme.size.smallIcon)
                    )
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.ConcertTabs(state: PagerState, onNavigate: (Int) -> Unit) {
        BoxWithConstraints(
            modifier = Modifier.widthIn(max = Theme.size.cell1 * 2)
                .fillMaxWidth()
                .padding(Theme.padding.value)
                .align(Alignment.CenterHorizontally)
                .clip(Theme.shape.circle)
                .background(Theme.color.primary.copy(alpha = 0.08f))
                .border(Theme.border.v7, Theme.color.outline.copy(alpha = 0.15f), Theme.shape.circle)
                .padding(Theme.padding.g5),
        ) {
            val tabWidth = maxWidth / 3
            val showIcons = tabWidth >= (Theme.size.input5 + Theme.padding.h) * maxOf(1f, LocalDensity.current.fontScale)

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
                        modifier = Modifier.weight(1f)
                            .clip(Theme.shape.circle)
                            .selectable(selected = selected, onClick = { if (!selected || state.isScrollInProgress) onNavigate(index) })
                            .pointerIcon(PointerIcon.Hand)
                            .heightIn(min = Theme.size.input8)
                            .padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showIcons) Icon(
                            icon = when (index) {
                                1 -> Icons.LibraryMusic
                                2 -> Icons.History
                                else -> Icons.EventAvailable
                            },
                            color = color,
                            modifier = Modifier.size(Theme.size.smallIcon)
                        )

                        SimpleClipText(
                            text = when (index) {
                                1 -> "歌曲"
                                2 -> "足迹"
                                else -> "Live"
                            },
                            color = color,
                            style = Theme.typography.v7.bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertDetailsContent(item: ConcertItem, pendingCid: Int?) {
        val deviceType by rememberDeviceType()

        BoxWithConstraints(
            modifier = (if (deviceType == Device.Type.PORTRAIT) Modifier.fillMaxWidth() else Modifier.width(Theme.size.cell1))
                .heightIn(max = Theme.size.cell1 * 2.5f)
                .fillMaxHeight(0.9f),
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
                    item(key = ItemKey("concert"), span = { GridItemSpan(maxLineSpan) }) {
                        ConcertCard(
                            item = item,
                            pendingCid = pendingCid,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item(key = ItemKey("setlist-title"), span = { GridItemSpan(maxLineSpan) }) {
                        SimpleEllipsisText(
                            text = "现场歌单 · ${item.songs.size} 首",
                            style = Theme.typography.v8.bold,
                            modifier = Modifier.padding(Theme.padding.value)
                        )
                    }

                    if (item.songs.isEmpty()) {
                        item(key = ItemKey("empty"), span = { GridItemSpan(maxLineSpan) }) {
                            SimpleEllipsisText(
                                text = "现场歌单还在整理中",
                                style = Theme.typography.v8,
                                modifier = Modifier.padding(Theme.padding.value)
                            )
                        }
                    }

                    items(count = item.songs.size, key = { it }, contentType = { "song" }) { index ->
                        ConcertSetlistRow(song = item.songs[index], sequence = index + 1)
                    }
                }
            }
        }
    }

    @Composable
    private fun ConcertListPage(memory: ConcertMemoryState, state: LazyGridState, pendingCid: Int?) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Theme.size.cell1 * maxOf(1f, LocalDensity.current.fontScale)),
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Theme.padding.eValue7,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.e7),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7),
        ) {
            item(key = ItemKey("overview"), span = { GridItemSpan(maxLineSpan) }) {
                ConcertSectionTitle(
                    title = "演出现场",
                    subtitle = "${memory.concerts.size} 场演出 · 已点亮 ${memory.attendedConcerts.size} 场"
                )
            }
            if (memory.concerts.isEmpty()) {
                item(key = ItemKey("empty"), span = { GridItemSpan(maxLineSpan) }) {
                    ConcertMessage(
                        title = "演出现场正在整理中",
                        description = "新的演出会在这里与你见面。"
                    )
                }
            }
            items(items = memory.concerts, key = { it.concert.cid }, contentType = { "concert" }) { item ->
                ConcertCard(
                    item = item,
                    pendingCid = pendingCid,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { openConcert(item) }
                )
            }
        }
    }

    @Composable
    private fun ConcertSongsPage(memory: ConcertMemoryState, state: LazyListState) {
        var selected by rememberValueState(0)
        val songs = when (selected) {
            1 -> memory.heardSongs
            2 -> memory.unheardSongs
            3 -> memory.uniqueSongs
            else -> memory.songs
        }

        Column(modifier = Modifier.fillMaxSize()) {
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
                onClick = { index, _ ->
                    if (index != selected) {
                        selected = index
                        state.requestScrollToItem(0)
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = state,
                contentPadding = Theme.padding.eValue7,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
            ) {
                if (songs.isEmpty()) {
                    item(key = ItemKey("empty")) {
                        ConcertMessage(
                            title = if (memory.songs.isEmpty()) "现场歌单正在整理中" else when (selected) {
                                1 -> "还没有听过的现场歌曲"
                                2 -> "所有歌曲都在你的记忆里了"
                                3 -> "你喜爱的每一首都并不唯一"
                                else -> "现场歌单正在整理中"
                            },
                            description = when (selected) {
                                1 -> "快去点亮一场看过的演出吧。"
                                3 -> "收录只在一场演出中出现过的歌曲。"
                                else -> "每一首歌，都可能在下一场与你相遇。"
                            },
                        )
                    }
                }

                items(items = songs, key = { it.song.sid }, contentType = { "song" }) { item ->
                    ConcertSongRow(item = item)
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
                        SimpleEllipsisText(
                            text = if (index == 0) "演出" else "歌曲",
                            color = color,
                            style = Theme.typography.v7.bold,
                            textAlign = TextAlign.Center
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
                                SimpleEllipsisText(
                                    text = memory.attendedConcerts.size.toString(),
                                    style = Theme.typography.v3.bold,
                                    color = Theme.color.primary,
                                    modifier = Modifier.alignByBaseline()
                                )
                                SimpleEllipsisText(
                                    text = "/ ${memory.concerts.size}",
                                    style = Theme.typography.v7,
                                    color = Theme.color.onSurfaceVariant,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }

                            SimpleEllipsisText(
                                text = "场演出看过",
                                style = Theme.typography.v8,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                SimpleEllipsisText(
                                    text = memory.heardSongs.size.toString(),
                                    style = Theme.typography.v3.bold,
                                    color = Theme.color.primary,
                                    modifier = Modifier.alignByBaseline()
                                )
                                SimpleEllipsisText(
                                    text = "/ ${memory.songs.size}",
                                    style = Theme.typography.v7,
                                    color = Theme.color.onSurfaceVariant,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }

                            SimpleEllipsisText(
                                text = "首不同的歌听过",
                                style = Theme.typography.v8,
                                textAlign = TextAlign.Center
                            )
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
        onExplore: () -> Unit,
    ) {
        var selected by rememberValueState(0)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(Theme.size.cell1 * maxOf(1f, LocalDensity.current.fontScale)),
            state = if (selected == 0) concertState else songState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Theme.padding.eValue7,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7),
            verticalArrangement = Arrangement.spacedBy(if (selected == 0) Theme.padding.e7 else Theme.padding.v7),
        ) {
            item(key = ItemKey("summary"), span = { GridItemSpan(maxLineSpan) }) {
                ConcertFootprintsSummary(
                    memory = memory,
                    selected = selected,
                    onSelect = { selected = it }
                )
            }

            if (selected == 0 && memory.attendedConcerts.isEmpty()) {
                item(key = ItemKey("empty"), span = { GridItemSpan(maxLineSpan) }) {
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
                    ConcertCard(
                        item = item,
                        pendingCid = pendingCid,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openConcert(item) }
                    )
                }
            }
            else if (memory.heardSongs.isEmpty()) {
                item(key = ItemKey("songs-empty"), span = { GridItemSpan(maxLineSpan) }) {
                    ConcertMessage(
                        title = if (memory.attendedConcerts.isEmpty()) "还没有听过的现场歌曲" else "现场歌单正在整理中",
                        description = if (memory.attendedConcerts.isEmpty()) "点亮看过的演出，就能收集它的现场歌曲。" else "补齐现场的歌单，会自动计入你的记忆。",
                        action = if (memory.attendedConcerts.isEmpty()) "去点亮现场" else null,
                        onAction = onExplore,
                    )
                }
            }
            else {
                items(items = memory.heardSongs, key = { "song-${it.song.sid}" }, span = { GridItemSpan(maxLineSpan) }, contentType = { "song" }) { item ->
                    ConcertSongRow(item = item, personal = true)
                }
            }
        }
    }

    @Composable
    override fun BasicContent() {
        val animationScope = rememberCoroutineScope()

        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            ConcertTabs(state = pagerState, onNavigate = { animationScope.launch { pagerState.animateScrollToPage(it) } })

            StatefulBox(provider = provider, modifier = Modifier.fillMaxWidth().weight(1f)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
                    when (page) {
                        0 -> ConcertListPage(memory = memory, state = concertState, pendingCid = pendingCid)
                        1 -> ConcertSongsPage(memory = memory, state = songState)
                        2 -> ConcertFootprintsPage(
                            memory = memory,
                            concertState = footprintsConcertState,
                            songState = footprintsSongState,
                            pendingCid = pendingCid,
                            onExplore = { animationScope.launch { pagerState.animateScrollToPage(0) } },
                        )
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
                        ConcertDetailsContent(item = item, pendingCid = pendingCid)
                    }
                }
                else ConcertMessage(title = "演出资料暂不可用", description = "关闭后刷新演出列表再试。")
            }
        }
    }
}
