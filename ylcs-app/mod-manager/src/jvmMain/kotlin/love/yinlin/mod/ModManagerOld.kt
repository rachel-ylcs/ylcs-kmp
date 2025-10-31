package love.yinlin.mod
//
//import androidx.compose.foundation.*
//import androidx.compose.foundation.draganddrop.dragAndDropTarget
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.selection.selectable
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Home
//import androidx.compose.material.icons.outlined.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draganddrop.DragAndDropEvent
//import androidx.compose.ui.draganddrop.DragAndDropTarget
//import androidx.compose.ui.draganddrop.awtTransferable
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.DpSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.util.fastForEach
//import androidx.compose.ui.util.fastForEachIndexed
//import androidx.compose.ui.util.fastJoinToString
//import androidx.compose.ui.window.WindowPosition
//import androidx.compose.ui.window.WindowState
//import androidx.compose.ui.window.singleWindowApplication
//import kotlinx.coroutines.launch
//import kotlinx.io.buffered
//import kotlinx.io.files.Path
//import kotlinx.io.files.SystemFileSystem
//import love.yinlin.data.Data
//import love.yinlin.data.music.MusicResourceType
//import love.yinlin.extension.fileSizeString
//import java.awt.datatransfer.DataFlavor
//import java.io.File
//import javax.swing.JFileChooser
//import javax.swing.filechooser.FileNameExtensionFilter
//import javax.swing.filechooser.FileSystemView
//
//
//private var lastChoosePath: File? = FileSystemView.getFileSystemView()?.homeDirectory
//
//private sealed interface Status {
//    val message: String
//    val color: Color
//
//    data object Idle : Status {
//        override val message: String = "空闲"
//        override val color: Color = Color.Magenta
//    }
//
//    data class Running(override val message: String) : Status {
//        override val color: Color = Color.Blue
//    }
//
//    data class Error(override val message: String) : Status {
//        override val color: Color = Color.Red
//    }
//
//    data object Completed : Status {
//        override val message: String = "完成"
//        override val color: Color = Color.Green
//    }
//}
//
//@Composable
//private fun StatusUI(
//    status: Status,
//    modifier: Modifier = Modifier
//) {
//    Text(
//        text = "状态: ${status.message}",
//        color = status.color,
//        modifier = modifier
//    )
//}
//
//@Composable
//private fun ClickButton(
//    text: String,
//    onClick: () -> Unit
//) {
//    Button(onClick = onClick) {
//        Text(text = text)
//    }
//}
//
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//private fun DragTextField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    title: String,
//    trailingIcon: @Composable (() -> Unit)? = null,
//    maxLines: Int = 1,
//    modifier: Modifier = Modifier,
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        label = { Text(text = title) },
//        trailingIcon = trailingIcon,
//        maxLines = maxLines,
//        modifier = modifier.dragAndDropTarget(
//            shouldStartDragAndDrop = {
//                it.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
//            },
//            target = remember { object : DragAndDropTarget {
//                override fun onDrop(event: DragAndDropEvent): Boolean {
//                    val list = event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
//                    val newValue = list.map { (it as File).toString() }.fastJoinToString("\n")
//                    onValueChange(newValue)
//                    return true
//                }
//            } }
//        )
//    )
//}
//
//@Composable
//private fun MergeUI(
//    modifier: Modifier = Modifier
//) {
//    val scope = rememberCoroutineScope()
//    var status: Status by remember { mutableStateOf(Status.Idle, referentialEqualityPolicy()) }
//    var input by remember { mutableStateOf("") }
//    var output by remember { mutableStateOf("") }
//    var filename by remember { mutableStateOf("${System.currentTimeMillis()}.rachel") }
//    val filters = remember { MusicResourceType.entries.map { false }.toMutableStateList() }
//
//    val reset = {
//        status = Status.Idle
//        input = ""
//        output = ""
//        filename = "${System.currentTimeMillis()}.rachel"
//        filters.fill(false)
//    }
//
//    Column(
//        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        Text(text = "从目录创建MOD", style = MaterialTheme.typography.titleLarge)
//        StatusUI(status = status, modifier = Modifier.fillMaxWidth())
//        Text(text = "每行包括一个媒体目录, 目录名为ID, 目录内包含若干资源")
//        DragTextField(
//            value = input,
//            onValueChange = { input = it },
//            title = "媒体目录",
//            trailingIcon = {
//                Icon(
//                    imageVector = Icons.Outlined.Search,
//                    contentDescription = null,
//                    modifier = Modifier.clickable {
//                        val chooser = JFileChooser().apply {
//                            isMultiSelectionEnabled = true
//                            currentDirectory = lastChoosePath
//                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
//                        }
//                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                            lastChoosePath = chooser.selectedFiles.firstOrNull()?.parentFile
//                            input = chooser.selectedFiles.joinToString("\n").trim()
//                        }
//                    }
//                )
//            },
//            maxLines = 5,
//            modifier = Modifier.fillMaxWidth()
//        )
//        Text(text = "一个单文件的具体路径, 会直接覆盖已有文件")
//        DragTextField(
//            value = output,
//            onValueChange = { output = it },
//            title = "保存文件路径",
//            trailingIcon = {
//                Row(modifier = Modifier.padding(horizontal = 10.dp)) {
//                    Icon(
//                        imageVector = Icons.Outlined.Search,
//                        contentDescription = null,
//                        modifier = Modifier.clickable {
//                            val chooser = JFileChooser().apply {
//                                isMultiSelectionEnabled = false
//                                currentDirectory = lastChoosePath
//                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
//                            }
//                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                                lastChoosePath = chooser.selectedFile
//                                output = chooser.selectedFile.absolutePath
//                            }
//                        }
//                    )
//                    Icon(
//                        imageVector = Icons.Outlined.Home,
//                        contentDescription = null,
//                        modifier = Modifier.clickable {
//                            output = "${System.getProperty("user.home")}${File.separator}Desktop"
//                        }
//                    )
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        )
//        OutlinedTextField(
//            value = filename,
//            onValueChange = { filename = it },
//            label = { Text(text = "保存文件名") },
//            maxLines = 1,
//            modifier = Modifier.fillMaxWidth()
//        )
//        Text(text = "过滤器")
//        FlowRow(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(10.dp),
//            verticalArrangement = Arrangement.spacedBy(10.dp),
//            itemVerticalAlignment = Alignment.CenterVertically,
//            maxItemsInEachRow = 5
//        ) {
//            MusicResourceType.entries.fastForEach { type ->
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(5.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.selectable(
//                        selected = filters[type.ordinal],
//                        onClick = { filters[type.ordinal] = !filters[type.ordinal] }
//                    )
//                ) {
//                    RadioButton(
//                        selected = filters[type.ordinal],
//                        onClick = null
//                    )
//                    Text(text = type.name)
//                }
//            }
//        }
//        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//            ClickButton("运行") {
//                if (status !is Status.Idle) {
//                    status = Status.Error("请先重置")
//                    return@ClickButton
//                }
//                val paths = input.split('\n')
//                if (paths.isEmpty() || output.isEmpty()) {
//                    status = Status.Error("媒体目录或保存路径为空")
//                    return@ClickButton
//                }
//                val mediaPaths = paths.map { Path(it) }
//                var saveName = filename.ifEmpty { "${System.currentTimeMillis()}.rachel" }
//                if (!saveName.endsWith(".rachel")) saveName += ".rachel"
//                val savePath = Path(output, saveName)
//                val filter = buildList {
//                    filters.fastForEachIndexed { index, v ->
//                        if (v) add(MusicResourceType.entries[index])
//                    }
//                }
//
//                scope.launch {
//                    val result = try {
//                        SystemFileSystem.sink(savePath).buffered().use { sink ->
//                            ModFactory.Merge(
//                                mediaPaths = mediaPaths,
//                                sink = sink
//                            ).process(
//                                filter = filter
//                            ) { progress, total, name ->
//                                status = Status.Running("运行中($progress/$total -> $name)")
//                            }
//                        }
//                    }
//                    catch (e: Throwable) {
//                        Data.Failure(throwable = e)
//                    }
//                    status = when (result) {
//                        is Data.Success -> Status.Completed
//                        is Data.Failure -> Status.Error("错误 -> ${result.throwable?.message}")
//                    }
//                }
//            }
//            ClickButton("重置", reset)
//        }
//    }
//}
//
//@Composable
//private fun ReleaseUI(
//    modifier: Modifier = Modifier
//) {
//    val scope = rememberCoroutineScope()
//    var status: Status by remember { mutableStateOf(Status.Idle, referentialEqualityPolicy()) }
//    var input by remember { mutableStateOf("") }
//    var output by remember { mutableStateOf("") }
//
//    val reset = {
//        status = Status.Idle
//        input = ""
//        output = ""
//    }
//
//    Column(
//        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        Text(text = "从MOD解析资源", style = MaterialTheme.typography.titleLarge)
//        StatusUI(status = status, modifier = Modifier.fillMaxWidth())
//        DragTextField(
//            value = input,
//            onValueChange = { input = it },
//            trailingIcon = {
//                Icon(
//                    imageVector = Icons.Outlined.Search,
//                    contentDescription = null,
//                    modifier = Modifier.clickable {
//                        val chooser = JFileChooser().apply {
//                            isMultiSelectionEnabled = false
//                            currentDirectory = lastChoosePath
//                            fileSelectionMode = JFileChooser.FILES_ONLY
//                            fileFilter = FileNameExtensionFilter("MOD文件", "rachel")
//                        }
//                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                            lastChoosePath = chooser.selectedFiles.firstOrNull()?.parentFile
//                            input = chooser.selectedFile.absolutePath
//                        }
//                    }
//                )
//            },
//            title = "MOD文件路径",
//            modifier = Modifier.fillMaxWidth()
//        )
//        Text(text = "导出目录, 每个媒体会单独在这个目录里创建子目录并释放资源")
//        DragTextField(
//            value = output,
//            onValueChange = { output = it },
//            title = "导出目录",
//            trailingIcon = {
//                Row(modifier = Modifier.padding(horizontal = 10.dp)) {
//                    Icon(
//                        imageVector = Icons.Outlined.Search,
//                        contentDescription = null,
//                        modifier = Modifier.clickable {
//                            val chooser = JFileChooser().apply {
//                                isMultiSelectionEnabled = false
//                                currentDirectory = lastChoosePath
//                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
//                            }
//                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                                lastChoosePath = chooser.selectedFile
//                                output = chooser.selectedFile.absolutePath
//                            }
//                        }
//                    )
//                    Icon(
//                        imageVector = Icons.Outlined.Home,
//                        contentDescription = null,
//                        modifier = Modifier.clickable {
//                            output = "${System.getProperty("user.home")}${File.separator}Desktop"
//                        }
//                    )
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        )
//        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//            ClickButton("运行") {
//                if (status !is Status.Idle) {
//                    status = Status.Error("请先重置")
//                    return@ClickButton
//                }
//                if (input.isEmpty() || output.isEmpty()) {
//                    status = Status.Error("MOD路径或导出目录为空")
//                    return@ClickButton
//                }
//
//                scope.launch {
//                    val result = try {
//                        SystemFileSystem.source(Path(input)).buffered().use { source ->
//                            ModFactory.Release(
//                                source = source,
//                                savePath = Path(output)
//                            ).process { progress, total, name ->
//                                status = Status.Running("运行中($progress/$total -> $name)")
//                            }
//                        }
//                    }
//                    catch (e: Throwable) {
//                        Data.Failure(throwable = e)
//                    }
//                    status = when (result) {
//                        is Data.Success -> Status.Completed
//                        is Data.Failure -> Status.Error("错误 -> ${result.throwable?.message}")
//                    }
//                }
//            }
//            ClickButton("重置", reset)
//        }
//    }
//}
//
//
//@Composable
//private fun PreviewUI(
//    modifier: Modifier = Modifier
//) {
//    val scope = rememberCoroutineScope()
//    var status: Status by remember { mutableStateOf(Status.Idle, referentialEqualityPolicy()) }
//    var input by remember { mutableStateOf("") }
//    var preview: ModFactory.Preview.PreviewResult? by remember { mutableStateOf(null, referentialEqualityPolicy()) }
//
//    val reset = {
//        status = Status.Idle
//        input = ""
//        preview = null
//    }
//
//    Column(
//        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(10.dp)
//    ) {
//        Text(text = "预览MOD", style = MaterialTheme.typography.titleLarge)
//        StatusUI(status = status, modifier = Modifier.fillMaxWidth())
//        DragTextField(
//            value = input,
//            onValueChange = { input = it },
//            trailingIcon = {
//                Icon(
//                    imageVector = Icons.Outlined.Search,
//                    contentDescription = null,
//                    modifier = Modifier.clickable {
//                        val chooser = JFileChooser().apply {
//                            isMultiSelectionEnabled = false
//                            currentDirectory = lastChoosePath
//                            fileSelectionMode = JFileChooser.FILES_ONLY
//                            fileFilter = FileNameExtensionFilter("MOD文件", "rachel")
//                        }
//                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                            lastChoosePath = chooser.selectedFiles.firstOrNull()?.parentFile
//                            input = chooser.selectedFile.absolutePath
//                        }
//                    }
//                )
//            },
//            title = "MOD文件路径",
//            modifier = Modifier.fillMaxWidth()
//        )
//        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
//            ClickButton("预览") {
//                if (status !is Status.Idle) {
//                    status = Status.Error("请先重置")
//                    return@ClickButton
//                }
//                if (input.isEmpty()) {
//                    status = Status.Error("MOD路径为空")
//                    return@ClickButton
//                }
//
//                scope.launch {
//                    val result = try {
//                        SystemFileSystem.source(Path(input)).buffered().use { source ->
//                            ModFactory.Preview(source = source).process()
//                        }
//                    }
//                    catch (e: Throwable) {
//                        Data.Failure(throwable = e)
//                    }
//                    status = when (result) {
//                        is Data.Success -> {
//                            preview = result.data
//                            Status.Completed
//                        }
//                        is Data.Failure -> Status.Error("错误 -> ${result.throwable?.message}")
//                    }
//                }
//            }
//            ClickButton("重置", reset)
//        }
//        preview?.let {
//            ModPreview(
//                preview = it,
//                modifier = Modifier.fillMaxWidth().weight(1f)
//            )
//        }
//    }
//}
//