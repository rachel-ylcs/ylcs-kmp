package love.yinlin.mod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.screen.CommonBasicScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.screen.resources.Res
import love.yinlin.compose.screen.resources.dialog_ok
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.LoadingBox
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.layout.SplitActionLayout
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dragAndDrop
import love.yinlin.data.mod.ModItem
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.*
import love.yinlin.mod.ModFactory.Preview.PreviewResult
import love.yinlin.platform.Coroutines
import org.jetbrains.compose.resources.stringResource

@Stable
class MainUI(manager: ScreenManager) : CommonBasicScreen(manager) {
    private var state by mutableStateOf(BoxState.EMPTY)
    private val library = mutableStateListOf<ModItem>()
    private val searchLibrary by derivedStateOf { library.filter { it.shown } }
    private val selectedLibrary by derivedStateOf { searchLibrary.filter { it.selected } }
    private val unSelectedLibrary by derivedStateOf { searchLibrary.filter { !it.selected } }
    private val status by derivedStateOf { "曲库总数: ${library.size}  |  已搜索: ${searchLibrary.size}  |  未选择: ${unSelectedLibrary.size}  |  已选择 ${selectedLibrary.size}" }

    private var onSearching by mutableStateOf(false)

    private val inputDialog = FloatingDialogInput(hint = "查找关键字", maxLength = 64)

    private suspend fun loadLibrary() {
        onSearching = false
        state = BoxState.LOADING
        val result = Coroutines.io {
            app.modPath.list().filter { it.isDirectory }.map { folder ->
                val id = folder.name
                var name = "未知"
                var enabled = true
                try {
                    require(Path(folder, ModResourceType.Record.filename).exists)
                    require(Path(folder, ModResourceType.Background.filename).exists)
                    require(Path(folder, ModResourceType.LineLyrics.filename).exists)
                    require(Path(folder, ModResourceType.Audio.filename).exists)
                    name = Path(folder, ModResourceType.Config.filename).readText().parseJsonValue<MusicInfo>()!!.name
                }
                catch (e: Throwable) {
                    e.printStackTrace()
                    enabled = false
                }
                ModItem(id = id, name = name, enabled = enabled)
            }
        }
        library.replaceAll(result)
        state = if (result.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
    }

    private fun startSearch(input: String) {
        onSearching = true
        var count = 0
        library.forEachIndexed { index, item ->
            if (item.name.contains(input, ignoreCase = true)) {
                if (!item.shown) library[index] = item.copy(shown = true)
                ++count
            }
            else {
                if (item.shown) library[index] = item.copy(shown = false)
            }
        }
        state = if (count == 0) BoxState.EMPTY else BoxState.CONTENT
    }

    private fun closeSearch() {
        onSearching = false
        library.forEachIndexed { index, item ->
            if (!item.shown) library[index] = item.copy(shown = true)
        }
        state = if (library.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
    }

    private suspend fun mergeSingleMod(
        filename: String,
        paths: List<Path>,
        filters: List<ModResourceType>,
        onProcess: (index: Int, total: Int, name: String) -> Unit
    ) {
        Path(app.outputPath, filename).write { sink ->
            ModFactory.Merge(paths, sink).process(filters = filters, onProcess = onProcess)
        }
    }

    private suspend fun runMergeTask(
        filters: List<ModResourceType>,
        mergeMode: Boolean,
        onProcess: (index: Int, total: Int, name: String) -> Unit
    ) {
        if (mergeMode) { // 多MOD合并
            // 检查MOD合法性
            val paths = selectedLibrary.filter { it.enabled }.map { Path(app.modPath, it.id) }
            // 生成随机名称文件
            mergeSingleMod(
                filename = "Merge${DateEx.CurrentLong}.${ModResourceType.MOD_EXT}",
                paths = paths,
                filters = filters,
                onProcess = onProcess
            )
        }
        else { // 多MOD分开打包
            for (item in selectedLibrary) {
                if (item.enabled) {
                    mergeSingleMod(
                        filename = "${item.name}.${ModResourceType.MOD_EXT}",
                        paths = listOf(Path(app.modPath, item.id)),
                        filters = filters,
                        onProcess = onProcess
                    )
                }
            }
        }
    }

    override suspend fun initialize() {
        loadLibrary()
    }

    @Composable
    private fun ModCard(modifier: Modifier = Modifier, item: ModItem) {
        ContextMenuArea(items = {
            listOf(
                ContextMenuItem("预览") {
                    modDetailsSheet.open(item)
                },
                ContextMenuItem("删除") {
                    launch {
                        if (slot.confirm.openSuspend(content = "真的要删除 ${item.name} 吗")) {
                            Path(app.modPath, item.id).deleteRecursively()
                            library.removeAll { it == item }
                        }
                    }
                }
            )
        }) {
            Box(modifier = modifier) {
                Column(modifier = Modifier.fillMaxWidth().clickable {
                    val index = library.indexOf(item)
                    library[index] = item.copy(selected = !item.selected)
                }.padding(CustomTheme.padding.equalValue)) {
                    LocalFileImage(
                        path = { Path(app.modPath, item.id, ModResourceType.Record.filename) },
                        item.id,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = item.name,
                        color = when {
                            !item.enabled -> MaterialTheme.colorScheme.onError
                            item.selected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().background(
                            when {
                                !item.enabled -> MaterialTheme.colorScheme.error
                                item.selected -> MaterialTheme.colorScheme.primaryContainer
                                else -> Colors.Transparent
                            }
                        ).padding(CustomTheme.padding.value)
                    )
                }
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                SplitActionLayout(
                    modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalSpace),
                    left = {
                        ActionSuspend(Icons.Outlined.Refresh, "刷新", onClick = ::loadLibrary)
                        if (onSearching) {
                            Action(Icons.Outlined.Close, "关闭搜索", onClick = ::closeSearch)
                        } else {
                            ActionSuspend(Icons.Outlined.Search, "搜索") {
                                inputDialog.openSuspend()?.let { input ->
                                    startSearch(input)
                                }
                            }
                        }
                    },
                    right = {
                        ActionSuspend(Icons.Outlined.Preview, "预览") {
                            previewSheet.open()
                        }
                        ActionSuspend(Icons.Outlined.Download, "导入") {
                            importSheet.open()
                        }
                        if (selectedLibrary.isNotEmpty()) {
                            Action(Icons.Outlined.Token, "打包") {
                                packageSheet.open()
                            }
                        }
                    }
                )
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                StatefulBox(
                    state = state,
                    modifier = Modifier.weight(2f).fillMaxHeight()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = unSelectedLibrary, key = { it.id }) {
                            ModCard(modifier = Modifier.fillMaxWidth(), item = it)
                        }
                    }
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight(), thickness = 10.dp)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    items(items = selectedLibrary, key = { it.id }) {
                        ModCard(modifier = Modifier.fillMaxWidth(), item = it)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface
            ) {
                Box(Modifier.fillMaxWidth().padding(CustomTheme.padding.value)) {
                    Text(
                        text = status,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    private val packageSheet = object : FloatingSheet() {
        override val initFullScreen: Boolean = true
        override val dismissOnBackPress: Boolean get() = !isRunning
        override val dismissOnClickOutside: Boolean get() = !isRunning

        private var isRunning by mutableStateOf(false)
        private val filters = mutableStateListOf<ModResourceType>()
        private var mergeMode by mutableStateOf(true)
        private val canSubmit by derivedStateOf { ModResourceType.BASE.all { it in filters } }

        private var statusText by mutableStateOf("正在打包中...")

        @Composable
        private fun SwitchText(type: ModResourceType) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = type in filters,
                    onCheckedChange = {
                        if (it) filters += type
                        else filters -= type
                    }
                )
                Text(
                    text = type.description,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        override suspend fun initialize() {
            isRunning = false
            filters.replaceAll(ModResourceType.BASE)
            mergeMode = true
        }

        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxSize().padding(end = CustomTheme.padding.horizontalSpace),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalExtraSpace),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "导出",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    ClickText(
                        text = stringResource(Res.string.dialog_ok),
                        enabled = canSubmit && !isRunning,
                        onClick = {
                            launch {
                                isRunning = true
                                catchingError {
                                    Coroutines.io {
                                        runMergeTask(filters, mergeMode) { index, total, name ->
                                            statusText = "[$index/$total] -> $name"
                                        }
                                    }
                                }?.let { statusText = it.message ?: "未知错误" }
                                isRunning = false
                                close()
                            }
                        }
                    )
                }
                if (isRunning) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        LoadingBox(text = statusText)
                    }
                }
                else {
                    Text(
                        text = "过滤器",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalExtraSpace),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
                    ) {
                        for (type in ModResourceType.entries) SwitchText(type)
                    }
                    Text(
                        text = "打包模式",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalExtraSpace),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = mergeMode,
                            onCheckedChange = { mergeMode = it }
                        )
                        Text(
                            text = "合并打包",
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    private val previewSheet = object : FloatingSheet() {
        override val initFullScreen: Boolean = true
        override val dismissOnBackPress: Boolean get() = !isRunning
        override val dismissOnClickOutside: Boolean get() = !isRunning

        private var isRunning by mutableStateOf(false)
        private var result: PreviewResult? by mutableStateOf(null)

        private var statusText by mutableStateOf("")

        override suspend fun initialize() {
            reset()
        }

        private fun reset() {
            isRunning = false
            result = null
            statusText = ""
        }

        @Composable
        private fun PreviewView(modifier: Modifier = Modifier, result: PreviewResult) {
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                item(-1) {
                    val metadata = result.metadata
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = CustomTheme.shadow.surface,
                        border = BorderStroke(width = CustomTheme.border.small, color = Colors.Gray3)
                    ) {
                        Column(
                            modifier = Modifier.padding(CustomTheme.padding.value),
                            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                        ) {
                            Text(
                                text = "MOD v${metadata.version}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(text = "媒体数: ${metadata.mediaNum}")
                            Text(text = "作者: ${metadata.info.author}")
                        }
                    }
                }
                items(
                    items = result.medias,
                    key = { it.id }
                ) { mediaItem ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = CustomTheme.shadow.surface,
                        border = BorderStroke(width = CustomTheme.border.small, color = Colors.Gray3)
                    ) {
                        Column(
                            modifier = Modifier.padding(CustomTheme.padding.extraValue),
                            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                        ) {
                            val config = mediaItem.config
                            if (config != null) {
                                Text(
                                    text = config.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(text = "版本: ${config.version}")
                                Text(text = "作者: ${config.author}")
                                Text(text = "ID: ${config.id}")
                                Text(text = "演唱: ${config.singer}")
                                Text(text = "作词: ${config.lyricist}")
                                Text(text = "作曲: ${config.composer}")
                                Text(text = "专辑: ${config.album}")
                                Text(text = "副歌点: ${config.chorus}")
                                Space()
                            }
                            Text(
                                text = "资源表",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            for ((resource, length) in mediaItem.resources) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().border(CustomTheme.border.small, Colors.Gray3).padding(CustomTheme.padding.value),
                                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
                                ) {
                                    Text(
                                        text = "${resource.description} (${resource.type})",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = remember(length) { length.toLong().fileSizeString })
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxSize().padding(end = CustomTheme.padding.horizontalSpace)
                    .dragAndDrop(!isRunning, DragFlag.File) { dropResult ->
                        if (dropResult is DropResult.File) {
                            val paths = dropResult.path
                            if (paths.size == 1) {
                                launch {
                                    reset()
                                    isRunning = true
                                    catchingError {
                                        result = Coroutines.io {
                                            paths[0].read { source -> ModFactory.Preview(source).process() }
                                        }
                                        statusText = ""
                                    }?.let { statusText = it.message ?: "未知错误" }
                                    isRunning = false
                                }
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
            ) {
                if (isRunning) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        LoadingBox(text = "正在解析中...")
                    }
                }
                else {
                    val previewResult = result
                    if (previewResult == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                EmptyBox(
                                    text = "拖拽最多一个MOD文件预览${if (statusText.isEmpty()) "" else "\nError: $statusText"}"
                                )
                            }
                        }
                    }
                    else {
                        PreviewView(
                            modifier = Modifier.fillMaxSize().padding(vertical = CustomTheme.padding.verticalSpace),
                            result = previewResult
                        )
                    }
                }
            }
        }
    }

    private val importSheet = object : FloatingSheet() {
        override val initFullScreen: Boolean = true
        override val dismissOnBackPress: Boolean get() = !isRunning
        override val dismissOnClickOutside: Boolean get() = !isRunning

        private var isRunning by mutableStateOf(false)

        private var statusText by mutableStateOf("正在解压中...")

        override suspend fun initialize() {
            isRunning = false
            statusText = ""
        }

        private suspend fun runReleaseTask(
            path: Path,
            onProcess: (index: Int, total: Int, id: String) -> Unit
        ) {
            path.read { source ->
                ModFactory.Release(source, app.modPath).process(onProcess)
            }
        }

        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxSize().padding(end = CustomTheme.padding.horizontalSpace)
                    .dragAndDrop(!isRunning, DragFlag.File) { dropResult ->
                        if (dropResult is DropResult.File) {
                            val paths = dropResult.path
                            if (paths.isNotEmpty()) {
                                launch {
                                    isRunning = true
                                    catchingError {
                                        Coroutines.io {
                                            for (path in paths) {
                                                runReleaseTask(path) { index, total, id ->
                                                    statusText = "[$index/$total] -> $id"
                                                }
                                            }
                                        }
                                    }?.let { statusText = it.message ?: "未知错误" }
                                    isRunning = false
                                    close()
                                    loadLibrary()
                                }
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace, Alignment.CenterVertically),
            ) {
                if (isRunning) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        LoadingBox(text = statusText)
                    }
                }
                else {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                        EmptyBox(text = "拖拽一个或多个MOD文件导入")
                    }
                }
            }
        }
    }

    private val modDetailsSheet = object : FloatingArgsSheet<ModItem>() {
        private var configText: String? by mutableStateOf(null)

        override suspend fun initialize(args: ModItem) {
            configText = Coroutines.io { Path(app.modPath, args.id, ModResourceType.Config.filename).readText() }
        }

        @Composable
        override fun Content(args: ModItem) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = CustomTheme.padding.verticalExtraSpace, end = CustomTheme.padding.horizontalExtraSpace),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocalFileImage(
                        path = { Path(app.modPath, args.id, ModResourceType.Record.filename) },
                        args.id,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    LocalFileImage(
                        path = { Path(app.modPath, args.id, ModResourceType.Background.filename) },
                        args.id,
                        modifier = Modifier.weight(1f).aspectRatio(0.5625f)
                    )
                }
                Text(
                    text = args.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = configText ?: "",
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }

    @Composable
    override fun Floating() {
        inputDialog.Land()
        packageSheet.Land()
        previewSheet.Land()
        importSheet.Land()
        modDetailsSheet.Land()
    }
}