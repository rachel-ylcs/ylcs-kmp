package love.yinlin.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogTemplate
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.SecondaryOutlinedButton
import love.yinlin.compose.ui.input.TertiaryButton
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.text.DateInput
import love.yinlin.compose.ui.text.DateInputState
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.concert.Concert
import love.yinlin.data.rachel.concert.ConcertSaveRequest
import love.yinlin.data.rachel.concert.ConcertSong
import love.yinlin.data.rachel.photo.PhotoAlbum
import love.yinlin.extension.DateEx
import love.yinlin.extension.cleaning
import love.yinlin.extension.then

@Stable
class ScreenConcertEditor : Screen() {
    override val title = "演出记忆数据库"

    @Stable
    private data class DraftSong(val sid: Int?, val name: String = "")

    @Stable
    private class Draft(concert: Concert?) {
        val cid = concert?.cid
        val title = InputState(concert?.title.orEmpty())
        val date = DateInputState(initValue = concert?.date?.let(DateEx.Formatter.standardDate::parse))
        val location = InputState(concert?.location.orEmpty())
        val formScroll = LazyGridState()

        var poster by mutableStateOf(concert?.poster)

        var songs by mutableRefStateOf<List<DraftSong>>(
            concert?.songs?.map { DraftSong(sid = it) } ?: []
        )

        val asRequest get() = ConcertSaveRequest(
            cid = cid,
            date = date.value?.let(DateEx.Formatter.standardDate::format).orEmpty(),
            location = location.text.trim().takeIf { it.isNotEmpty() },
            title = title.text.trim(),
            poster = poster,
            songIds = songs.map { it.sid },
            newSongNames = songs.filter { it.sid == null }.map { it.name },
        )
    }

    @Stable
    private enum class EditorPane { Form, Songs, Albums, Photos }

    private val order = compareByDescending<Concert> { it.date }.thenByDescending { it.cid }
    private var concerts: List<Concert> by mutableRefStateOf([])
    private var songs: Map<Int, ConcertSong> by mutableRefStateOf(emptyMap())
    private var albums: List<PhotoAlbum> by mutableRefStateOf([])
    private var saving: Boolean by mutableStateOf(false)
    private val failedDrafts = mutableMapOf<Int?, Draft>()
    private val provider = RachelStatefulProvider(emptyText = "暂无演出记录")

    override fun initialize() {
        launch {
            provider.withLoading {
                ApiConcertGetEditorData.request(app.config.userToken) { concertList, songList, albumList ->
                    concerts = concertList.sortedWith(order)
                    songs = songList.associateBy { it.sid }
                    albums = albumList.sortedWith(compareByDescending<PhotoAlbum> { it.ts }.thenByDescending { it.aid })
                }?.then { throw it }
                concerts.isNotEmpty()
            }
        }
    }

    private fun openEditor(concert: Concert? = null) {
        if (saving || editorDialog.isOpen || provider.isLoading || provider.status == StatefulStatus.NetworkError) return

        launch {
            val draft = failedDrafts.remove(concert?.cid) ?: Draft(concert)
            val request = editorDialog.open(draft) ?: return@launch
            saving = true

            cleaning({
                val error = ApiConcertSaveConcert.request(app.config.userToken, request) { saved, setlist ->
                    songs = songs + setlist.associateBy { it.sid }
                    concerts = (concerts.filter { it.cid != saved.cid } + saved).sortedWith(order)
                    provider.status = StatefulStatus.Content
                    slot.tip.success(if (request.cid == null) "演出已新增" else "演出已保存")
                }.errorTip

                if (error != null) failedDrafts[request.cid] = draft
            }) {
                saving = false
            }
        }
    }

    private fun posterUrl(poster: String?): String? {
        if (poster == null) return null
        val parts = poster.split('/', limit = 2)

        val index = parts.getOrNull(1)?.toIntOrNull() ?: return null

        return albums.firstOrNull { (val name, val picNum) ->
            name == parts[0] && index in 0 ..<picNum
        }?.thumbPath(index)?.url
    }

    @Composable
    private fun SongTile(song: ConcertSong?, title: String, onActivate: () -> Unit) {
        val pid = song?.pid

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shape.v7,
            contentPadding = Theme.padding.eValue,
            tonalLevel = 1,
            onClick = onActivate
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                val cover = Modifier.size(Theme.size.image8)
                WebImage(
                    source = pid?.let { ServerRes.Mod.Song(it).res(ModResourceType.Record.filename).url },
                    modifier = cover.clip(Theme.shape.v7),
                    contentScale = ContentScale.Crop,
                )
                SimpleEllipsisText(text = title, style = Theme.typography.v8)
            }
        }
    }

    @Composable
    private fun CatalogRow(
        title: String,
        url: String?,
        onClick: (() -> Unit)?,
        caption: String? = null,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Theme.shape.v7,
            contentPadding = Theme.padding.value,
            tonalLevel = 1,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WebImage(
                    source = url,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image8).clip(Theme.shape.v7),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    SimpleEllipsisText(text = title, style = Theme.typography.v7.bold)
                    if (caption != null) SimpleEllipsisText(text = caption, style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                }
            }
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        Icon(
            icon = Icons.Add,
            enabled = !saving && !provider.isLoading && provider.status != StatefulStatus.NetworkError,
            onClick = ::openEditor,
        )
    }

    @Composable
    override fun Content() {
        StatefulBox(provider = provider, modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            ) {
                items(items = concerts, key = { it.cid }) { concert ->
                    CatalogRow(
                        title = concert.title,
                        url = posterUrl(concert.poster),
                        caption = concert.location?.takeIf { it.isNotBlank() }?.let { "${concert.date} · $it" } ?: concert.date,
                        onClick = if (saving) null else ({ openEditor(concert) }),
                    )
                }
            }
        }
    }

    private val editorDialog = this land object : DialogTemplate<ConcertSaveRequest>() {
        override val scrollable = false

        var draft by mutableRefStateOf<Draft?>(null)
        var pane by mutableStateOf(EditorPane.Form)
        var album by mutableRefStateOf<PhotoAlbum?>(null)
        var validation by mutableStateOf<String?>(null)
        val songSearch = InputState(maxLength = 32)
        val albumSearch = InputState()

        fun songName(entry: DraftSong): String? = if (entry.sid == null) entry.name else songs[entry.sid]?.name

        fun hasSong(draft: Draft, song: DraftSong): Boolean {
            val name = songName(song)
            return draft.songs.any { entry ->
                (song.sid != null && entry.sid == song.sid) || (name != null && name.equals(songName(entry), ignoreCase = true))
            }
        }

        suspend fun open(draft: Draft): ConcertSaveRequest? {
            this.draft = draft
            pane = EditorPane.Form
            validation = null
            album = null
            songSearch.clear()
            albumSearch.clear()

            return awaitResult()
        }

        private fun submit(draft: Draft) {
            val request = draft.asRequest

            validation = when {
                request.title.isBlank() -> "请输入演出标题"
                request.poster != null && posterUrl(request.poster) == null -> "请重新选择或清空已失效的海报"
                request.songIds.any { it != null && it !in songs } -> "请移除歌单中已找不到的歌曲"
                else -> null
            }

            if (validation == null) future?.send(request)
        }

        private fun addSong(draft: Draft, song: DraftSong) {
            if (hasSong(draft, song)) return

            draft.songs += song
            songSearch.clear()
            pane = EditorPane.Form
        }

        override val actions: @Composable (RowScope.() -> Unit) = {
            val current = draft
            if (current != null) {
                when (pane) {
                    EditorPane.Form -> {
                        PrimaryButton(
                            text = if (current.cid == null) "新增" else "保存",
                            icon = if (current.cid == null) Icons.Add else Icons.Save,
                            onClick = { submit(current) }
                        )
                        SecondaryOutlinedButton(
                            text = "取消",
                            icon = Icons.Clear,
                            onClick = ::close
                        )
                    }
                    EditorPane.Songs, EditorPane.Albums -> {
                        SecondaryButton(
                            text = "返回表单",
                            icon = Icons.ArrowBack,
                            onClick = { pane = EditorPane.Form }
                        )
                    }
                    EditorPane.Photos -> {
                        SecondaryButton(
                            text = "返回图集",
                            icon = Icons.ArrowBack,
                            onClick = { pane = EditorPane.Albums }
                        )
                    }
                }
            }
        }

        @Composable
        private fun Form(draft: Draft) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell7),
                modifier = Modifier.fillMaxWidth(),
                state = draft.formScroll,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
            ) {
                val maxSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

                validation?.let {
                    item(key = ItemKey("ErrorText"), span = maxSpan) {
                        SimpleEllipsisText(text = it, color = Theme.color.error)
                    }
                }

                item(key = ItemKey("Title"), span = maxSpan) {
                    Input(state = draft.title, hint = "演出标题", modifier = Modifier.fillMaxWidth())
                }

                item(key = ItemKey("Date"), span = maxSpan) {
                    DateInput(state = draft.date, hint = "演出日期", modifier = Modifier.fillMaxWidth())
                }

                item(key = ItemKey("Location"), span = maxSpan) {
                    Input(state = draft.location, hint = "地点（可选）", modifier = Modifier.fillMaxWidth())
                }

                item(key = ItemKey("Poster"), span = maxSpan) {
                    Row(
                        modifier = Modifier.fillMaxWidth().dashBorder(Theme.border.v7, Theme.color.tertiary, Theme.shape.v5).padding(Theme.padding.e),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val poster = draft.poster

                        WebImage(
                            source = posterUrl(poster),
                            modifier = Modifier.weight(1f).aspectRatio(2f).clip(Theme.shape.v5),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier.padding(Theme.padding.eValue),
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                        ) {
                            Icon(icon = Icons.Refresh, onClick = { pane = EditorPane.Albums })
                            if (poster != null) Icon(icon = Icons.Clear, onClick = { draft.poster = null })
                        }
                    }
                }

                item(key = ItemKey("Playlist"), span = maxSpan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(text = "歌单 (${draft.songs.size}首)", style = Theme.typography.v7.bold)
                        Icon(icon = Icons.Add, onClick = { pane = EditorPane.Songs })
                    }
                }

                itemsIndexed(items = draft.songs) { index, song ->
                    SongTile(
                        song = song.sid?.let { songs[it] },
                        title = songName(song) ?: "#${song.sid}",
                        onActivate = {
                            draft.songs = draft.songs.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
            }
        }

        @Composable
        private fun SongPicker(draft: Draft) {
            val keyword = songSearch.text.trim()

            val matching = remember(songs, keyword) {
                songs.values
                    .filter { it.name.contains(keyword, ignoreCase = true) }
                    .sortedWith(compareBy<ConcertSong> { it.pid.isNullOrBlank() }.thenByDescending { it.sid })
            }

            val available = remember(draft.songs, songs, matching) {
                matching.filterNot { hasSong(draft, DraftSong(sid = it.sid)) }
            }

            val manualSong = DraftSong(sid = null, name = keyword)
            val alreadyAdded = keyword.isNotEmpty() && hasSong(draft, manualSong)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Input(state = songSearch, hint = "搜索或输入歌曲名称", modifier = Modifier.fillMaxWidth())

                if (alreadyAdded) SimpleEllipsisText(text = "这首歌已在歌单中", color = Theme.color.warning)
                else if (keyword.isNotEmpty() && matching.none { it.name.equals(keyword, ignoreCase = true) }) {
                    TertiaryButton(
                        text = "添加未收录的「$keyword」到演出曲库",
                        onClick = { addSong(draft, manualSong) }
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.cell7),
                    modifier = Modifier.fillMaxWidth().weight(1f, false),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                ) {
                    items(items = available, key = { it.sid }) { song ->
                        SongTile(
                            song = song,
                            title = song.name,
                            onActivate = { addSong(draft, DraftSong(sid = song.sid)) }
                        )
                    }
                }
            }
        }

        @Composable
        private fun AlbumPicker() {
            val keyword = albumSearch.text.trim()

            val matching = remember(albums, keyword) {
                albums.filter { it.picNum > 0 && (it.name.contains(keyword, true) || it.title.contains(keyword, true)) }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Input(state = albumSearch, hint = "搜索图集名或标题", modifier = Modifier.fillMaxWidth())

                if (matching.isEmpty()) SimpleEllipsisText(text = "没有可选的图集", color = Theme.color.warning)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, false),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    items(items = matching, key = { it.aid }) { item ->
                        CatalogRow(
                            title = item.title,
                            url = item.thumbPath(0).url,
                            caption = item.name,
                            onClick = {
                                album = item
                                pane = EditorPane.Photos
                            }
                        )
                    }
                }
            }
        }

        @Composable
        private fun PhotoPicker(draft: Draft, selectedAlbum: PhotoAlbum) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.image7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e9),
            ) {
                items(count = selectedAlbum.picNum) { index ->
                    val poster = "${selectedAlbum.name}/$index"
                    val selected = draft.poster == poster
                    val shape = Theme.shape.v7

                    WebImage(
                        source = selectedAlbum.thumbPath(index).url,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(shape)
                            .condition(selected) {
                                border(Theme.border.v5, Theme.color.primary, shape)
                            },
                        onClick = {
                            draft.poster = poster
                            pane = EditorPane.Form
                        }
                    )
                }
            }
        }

        @Composable
        override fun Land() {
            val current = draft ?: return

            val selectedAlbum = album
            val heading = when (pane) {
                EditorPane.Form -> current.cid?.let { "编辑演出 #$it" } ?: "新增演出"
                EditorPane.Songs -> "添加歌曲"
                EditorPane.Albums -> "选择图集"
                EditorPane.Photos -> selectedAlbum?.title ?: "选择海报"
            }

            LandDialogTemplate(heading) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    when (pane) {
                        EditorPane.Form -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                            ) {

                                Form(current)
                            }
                        }
                        EditorPane.Songs -> SongPicker(current)
                        EditorPane.Albums -> AlbumPicker()
                        EditorPane.Photos -> {
                            if (selectedAlbum != null) PhotoPicker(current, selectedAlbum)
                        }
                    }
                }
            }
        }
    }
}
