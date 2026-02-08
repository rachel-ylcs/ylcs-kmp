package love.yinlin.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.DefaultStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.VerticalDivider
import love.yinlin.compose.ui.mod.ModPreviewLayout
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.node.dragAndDrop
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.window.ContextMenu
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModItem
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.*
import love.yinlin.mod.ModFactory
import java.awt.Desktop
import java.io.File

@Stable
class ScreenMain : BasicScreen() {
    private val boxStatus = DefaultStatefulProvider()
    private val library = mutableStateListOf<ModItem>()
    private val searchLibrary by derivedStateOf { library.filter { it.shown } }
    private val selectedLibrary by derivedStateOf { searchLibrary.filter { it.selected } }
    private val unSelectedLibrary by derivedStateOf { searchLibrary.filter { !it.selected } }
    private val status by derivedStateOf { "曲库总数: ${library.size}  |  已搜索: ${searchLibrary.size}  |  未选择: ${unSelectedLibrary.size}  |  已选择 ${selectedLibrary.size}" }

    private var onSearching by mutableStateOf(false)

    private val isSelectAll by derivedStateOf { unSelectedLibrary.isEmpty() && selectedLibrary.isNotEmpty() }

    private val leftState = LazyGridState()
    private val rightState = LazyGridState()

    private suspend fun loadLibrary() {
        onSearching = false
        boxStatus.status = StatefulStatus.Loading
        val result = Coroutines.io {
            app.libraryPath.list().filter { it.isDirectory }.map { folder ->
                var id = ""
                var name = "未知"
                var enabled = true
                catchingError {
                    require(Path(folder, ModResourceType.Record.filename).exists)
                    require(Path(folder, ModResourceType.Background.filename).exists)
                    require(Path(folder, ModResourceType.LineLyrics.filename).exists)
                    require(Path(folder, ModResourceType.Audio.filename).exists)
                    val musicInfo =
                        Path(folder, ModResourceType.Config.filename).readText()!!.parseJsonValue<MusicInfo>()
                    id = musicInfo.id
                    name = musicInfo.name
                }?.let {
                    it.printStackTrace()
                    enabled = false
                }
                ModItem(id = id, name = name, path = folder, enabled = enabled)
            }.distinctBy { it.id }
        }
        library.replaceAll(result)
        boxStatus.status = if (result.isEmpty()) StatefulStatus.Empty else StatefulStatus.Content
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
        boxStatus.status = if (count == 0) StatefulStatus.Empty else StatefulStatus.Content
    }

    private fun closeSearch() {
        onSearching = false
        library.forEachIndexed { index, item ->
            if (!item.shown) library[index] = item.copy(shown = true)
        }
        boxStatus.status = if (library.isEmpty()) StatefulStatus.Empty else StatefulStatus.Content
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
            val paths = selectedLibrary.filter { it.enabled }.map { it.path }
            // 生成随机名称文件
            mergeSingleMod(
                filename = "Merge_${paths.size}_${DateEx.CurrentLong}.${ModResourceType.MOD_EXT}",
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
                        paths = listOf(item.path),
                        filters = filters,
                        onProcess = onProcess
                    )
                }
            }
        }
    }

    private suspend fun deploy(items: List<ModItem>) {
        catchingError {
            Coroutines.io {
                val modPath = app.modPath
                modPath.deleteRecursively()
                modPath.mkdir()
                for (item in items) {
                    val itemPath = Path(modPath, item.id)
                    itemPath.mkdir()
                    // 复制基础资源
                    for (resPath in item.path.list()) {
                        val type = ModResourceType.fromType(resPath.nameWithoutExtension)!!
                        if (type in ModResourceType.DEPLOYMENT) resPath.writeTo(Path(itemPath, resPath.name))
                    }
                    // 基础资源打包
                    Path(itemPath, ModResourceType.BASE_RES).write { sink ->
                        ModFactory.Merge(listOf(item.path), sink)
                            .process(filters = ModResourceType.BASE) { _, _, _ -> }
                    }
                }
            }
            slot.tip.success("部署成功")
        }.errorTip
    }

    override suspend fun initialize() {
        loadLibrary()
    }

    @Composable
    private fun ModCard(modifier: Modifier = Modifier.Companion, item: ModItem) {
        val needRename = item.path.name != item.name

        ContextMenu(menus = {
            item("预览") { modDetailsSheet.open(item) }
            item("删除") {
                launch {
                    if (slot.confirm.open(content = "真的要删除 ${item.name} 吗")) {
                        item.path.deleteRecursively()
                        library.removeAll { it == item }
                    }
                }
            }
            if (needRename) {
                item("重命名") {
                    launch {
                        item.path.rename(item.name)?.let { loadLibrary() }
                    }
                }
            }
            item("打开所在目录") { Desktop.getDesktop().open(File(item.path.toString())) }
            item("音游编辑") {
                launch { navigate(::ScreenRhyme, item.path.toString()) }
            }
        }) {
            Column(modifier = modifier.clickable {
                val index = library.indexOf(item)
                library[index] = item.copy(selected = !item.selected)
            }.padding(Theme.padding.eValue)) {
                LocalFileImage(
                    path = { Path(item.path, ModResourceType.Record.filename) },
                    item.id,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = item.name,
                    color = when {
                        !item.enabled -> Theme.color.onError
                        item.selected -> Theme.color.onContainer
                        else -> Theme.color.onSurface
                    },
                    style = if (needRename) Theme.typography.v7.bold else Theme.typography.v7,
                    textDecoration = if (needRename) TextDecoration.LineThrough else null,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().background(
                        when {
                            !item.enabled -> Theme.color.error
                            item.selected -> Theme.color.primaryContainer
                            else -> Colors.Transparent
                        }
                    ).padding(Theme.padding.value)
                )
            }
        }
    }

    @Composable
    private fun ActionBar(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shadowElevation = Theme.shadow.v3
        ) {
            ActionScope.SplitContainer(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                left = {
                    LoadingIcon(Icons.Refresh, tip = "刷新", onClick = ::loadLibrary)
                    if (onSearching) Icon(Icons.Clear, tip = "关闭搜索", onClick = ::closeSearch)
                    else LoadingIcon(Icons.Search, tip = "搜索", onClick = {
                        inputDialog.open()?.let { startSearch(it) }
                    })
                    Icon(Icons.SelectAll, tip = if (isSelectAll) "取消全选" else "全选", onClick = {
                        val v = isSelectAll
                        library.fastForEachIndexed { index, item ->
                            if (item.selected == v) library[index] = item.copy(selected = !v)
                        }
                    })
                },
                right = {
                    Icon(Icons.MusicNote, tip = "音游配置", onClick = { navigate(::ScreenRhyme, null)} )
                    Icon(Icons.Preview, tip = "预览", onClick = { previewSheet.open() })
                    Icon(Icons.Download, tip = "导入", onClick = { importSheet.open() })
                    if (selectedLibrary.isNotEmpty()) {
                        Icon(Icons.Token, tip = "打包", onClick = { packageSheet.open() })
                        LoadingIcon(Icons.LocalAirport, tip = "部署", onClick = {
                            if (slot.confirm.open(content = "部署所选MOD到mod目录吗?")) {
                                deploy(selectedLibrary)
                            }
                        })
                    }
                }
            )
        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.value,
            contentAlignment = Alignment.CenterStart,
            shadowElevation = Theme.shadow.v3
        ) {
            SimpleEllipsisText(status)
        }
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.fillMaxSize()) {
            ActionBar(modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                StatefulBox(
                    provider = boxStatus,
                    modifier = Modifier.weight(2f).fillMaxHeight()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (unSelectedLibrary.isEmpty()) {
                            Text(
                                text = "待选择区域",
                                color = Theme.color.onSurface,
                                style = Theme.typography.v6.bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(Theme.size.cell5),
                                modifier = Modifier.fillMaxSize(),
                                state = leftState
                            ) {
                                items(items = unSelectedLibrary, key = { it.id }) {
                                    ModCard(modifier = Modifier.fillMaxWidth(), item = it)
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxHeight()) {
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().zIndex(1f),
                        thickness = Theme.border.v1,
                        color = Colors.White
                    )
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().zIndex(2f),
                        adapter = rememberScrollbarAdapter(scrollState = leftState)
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (selectedLibrary.isEmpty()) {
                        Text(
                            text = "已选择区域",
                            color = Theme.color.onSurface,
                            style = Theme.typography.v6.bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(Theme.size.cell5),
                            modifier = Modifier.fillMaxSize(),
                            state = rightState
                        ) {
                            items(items = selectedLibrary, key = { it.id }) {
                                ModCard(modifier = Modifier.fillMaxWidth(), item = it)
                            }
                        }
                    }
                }
            }
            StatusBar(modifier = Modifier.fillMaxWidth())
        }
    }

    private val inputDialog = this land DialogInput(hint = "查找关键字", maxLength = 64)

    private val previewSheet = this land object : Sheet() {
        override val dismissOnBackPress: Boolean get() = !isRunning
        override val dismissOnClickOutside: Boolean get() = !isRunning

        private var isRunning by mutableStateOf(false)
        private var result: ModFactory.Preview.PreviewResult? by mutableStateOf(null)

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
        override fun Content() {
            Box(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9).dragAndDrop(!isRunning, DragFlag.File) { dropResult ->
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
            }) {
                if (isRunning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        CircleLoading.Content()
                        Text("正在解析中...")
                    }
                } else {
                    val previewResult = result
                    if (previewResult == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(2f).dashBorder(Theme.border.v6, Theme.color.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "拖拽最多一个MOD文件预览${if (statusText.isEmpty()) "" else "\nError: $statusText"}",
                                color = Theme.color.primary,
                                style = Theme.typography.v6.bold
                            )
                        }
                    } else {
                        ModPreviewLayout(
                            modifier = Modifier.fillMaxWidth(),
                            result = previewResult
                        )
                    }
                }
            }
        }
    }

    private val importSheet = this land object : Sheet() {
        override val dismissOnBackPress: Boolean by derivedStateOf { !isRunning }
        override val dismissOnClickOutside: Boolean by derivedStateOf { !isRunning }

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
                ModFactory.Release(source, app.libraryPath).process(onProcess)
            }
        }

        @Composable
        override fun Content() {
            Box(modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9).dragAndDrop(!isRunning, DragFlag.File) { dropResult ->
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
            }) {
                if (isRunning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        CircleLoading.Content()
                        Text(statusText)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f).dashBorder(Theme.border.v6, Theme.color.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "拖拽一个或多个MOD文件导入",
                            color = Theme.color.primary,
                            style = Theme.typography.v6.bold
                        )
                    }
                }
            }
        }
    }

    private val modDetailsSheet = this land object : SheetContent<ModItem>() {
        private var configText: String? by mutableStateOf(null)

        override suspend fun initialize(args: ModItem) {
            configText = Coroutines.io { Path(args.path, ModResourceType.Config.filename).readText() }
        }

        @Composable
        override fun Content(args: ModItem) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocalFileImage(
                        path = { Path(args.path, ModResourceType.Record.filename) },
                        args.id,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    LocalFileImage(
                        path = { Path(args.path, ModResourceType.Background.filename) },
                        args.id,
                        modifier = Modifier.weight(1f).aspectRatio(0.5625f)
                    )
                }
                SimpleEllipsisText(args.name, style = Theme.typography.v6.bold, color = Theme.color.primary)
                Text(
                    text = configText ?: "",
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }

    private val packageSheet = this land object : Sheet() {
        override val dismissOnBackPress: Boolean by derivedStateOf { !isRunning }
        override val dismissOnClickOutside: Boolean by derivedStateOf { !isRunning }

        private var isRunning by mutableStateOf(false)
        private val filters = mutableStateListOf<ModResourceType>()
        private var mergeMode by mutableStateOf(true)
        private val canSubmit by derivedStateOf { ModResourceType.BASE.all { it in filters } }

        private var statusText by mutableStateOf("正在打包中...")

        @Composable
        private fun SwitchText(type: ModResourceType) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = type in filters,
                    onCheckedChange = {
                        if (it) filters += type
                        else filters -= type
                    }
                )
                SimpleEllipsisText(type.description, color = Theme.color.onSurface)
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
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Theme.padding.v9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "导出",
                        style = Theme.typography.v6.bold,
                        color = Theme.color.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryTextButton(
                        text = "确认",
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        CircleLoading.Content()
                        Text(statusText)
                    }
                } else {
                    SimpleEllipsisText("过滤器", color = Theme.color.onSurface)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Theme.padding.v9),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        for (type in ModResourceType.entries) SwitchText(type)
                    }
                    SimpleEllipsisText("打包模式", color = Theme.color.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Theme.padding.v9),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = mergeMode,
                            onCheckedChange = { mergeMode = it }
                        )
                        SimpleEllipsisText("合并打包", color = Theme.color.onSurface)
                    }
                }
            }
        }
    }
}