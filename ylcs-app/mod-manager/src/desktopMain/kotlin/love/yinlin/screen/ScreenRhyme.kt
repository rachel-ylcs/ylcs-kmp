package love.yinlin.screen

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.ZoomWebImage
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.input.SingleSelector
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.Space
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.Chorus
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLine
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.*
import love.yinlin.mod_manager.resources.Res
import love.yinlin.mod_manager.resources.music
import love.yinlin.util.QrcDecrypter
import org.jetbrains.compose.resources.Font
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File

@Stable
class ScreenRhyme(manager: ScreenManager, private val path: String?) : Screen(manager) {
    private val prettyJson = Json { prettyPrint = true }
    private val defaultConfig = RhymeLyricsConfig(id = "", duration = 0L, chorus = emptyList(), lyrics = emptyList(), offset = 0)

    private val useFile by derivedStateOf { path != null }
    private var name by mutableStateOf("未知歌曲")
    private var rhymeConfig by mutableRefStateOf(defaultConfig)
    private val rhymeConfigText by derivedStateOf { prettyJson.encodeToString(rhymeConfig) }

    private var selectedTab by mutableStateOf("歌词")

    private var notationImage by mutableRefStateOf<String?>(null)

    private var copyData by mutableRefStateOf<List<RhymeAction>?>(null)

    private fun parseNrc(nrc: String) {

    }

    private fun parseQrc(qrc: String) {
        val lyrics = qrc.substringAfter("LyricContent=\"").substringBeforeLast("\"/>")
        val lines = lyrics.split("\n")

        val rhymeLines = mutableListOf<RhymeLine>()
        val headerRegex = "\\[\\s*(\\d+),\\s*(\\d+)\\s*](.*)".toRegex()
        val bodyRegex = "([^()]+?)\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)".toRegex()
        for (line in lines) {
            val headerMatch = headerRegex.find(line) ?: continue
            val lineStart = headerMatch.groupValues[1].toLong()
            val rawBody = headerMatch.groupValues[3]
            val matches = bodyRegex.findAll(rawBody)

            val theme = mutableListOf<RhymeAction>()
            val lineText = StringBuilder()
            for (match in matches) {
                val ch = match.groupValues[1]
                if (ch.length != 1) continue
                lineText.append(ch)
                theme += RhymeAction.Note(
                    ch = ch,
                    end = (match.groupValues[2].toLong() + match.groupValues[3].toLong() - lineStart).toInt(),
                    scale = 1
                )
            }
            rhymeLines += RhymeLine(
                text = lineText.toString(),
                start = lineStart,
                theme = theme
            )
        }

        rhymeConfig = rhymeConfig.copy(lyrics = rhymeLines)
        slot.tip.success("解析成功")
    }

    override val title: String by derivedStateOf { name }

    private suspend fun saveConfig() {
        path?.let {
            if (!slot.confirm.openSuspend(content = "替换音游配置到库")) return
            val rhymePath = Path(it, ModResourceType.Rhyme.filename)
            rhymePath.writeText(prettyJson.encodeToString(rhymeConfig))
            slot.tip.success("保存成功")
        }
    }

    private suspend fun deleteConfig() {
        path?.let {
            if (!slot.confirm.openSuspend(content = "删除音游配置")) return
            val rhymePath = Path(it, ModResourceType.Rhyme.filename)
            rhymePath.delete()
            pop()
        }
    }

    override suspend fun initialize() {
        catchingError {
            rhymeConfig = Coroutines.io {
                path?.let {
                    val musicInfo = Path(it, ModResourceType.Config.filename).readText()!!.parseJsonValue<MusicInfo>()
                    val rhymePath = Path(it, ModResourceType.Rhyme.filename)
                    val newConfig = RhymeLyricsConfig(
                        id = musicInfo.id,
                        duration = 0L,
                        chorus = musicInfo.chorus?.map { v -> Chorus(v, v) } ?: emptyList(),
                        lyrics = emptyList(),
                        offset = 0,
                    )
                    if (!rhymePath.exists) rhymePath.writeText(prettyJson.encodeToString(newConfig))
                    name = musicInfo.name
                    rhymePath.readText()!!.parseJsonValue()
                } ?: run {
                    name = "未知歌曲"
                    defaultConfig
                }
            }
        }.errorTip
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.Done, enabled = useFile, onClick = ::saveConfig)
        ActionSuspend(Icons.Outlined.Delete, enabled = useFile, onClick = ::deleteConfig)
    }

    private val scaleTable = arrayOf(
        "9",
        "1", "2", "3", "4", "5", "6", "7",
        "\uF021", "@", "#", "$", "\u00A7", "\u00A8", "\u00A9",
        "\u0086", "\u0087", "\u0088", "*", "%", "^", "&",
    )

    @Composable
    private fun LyricsLineEditor(
        modifier: Modifier = Modifier,
        config: RhymeLyricsConfig,
        lineIndex: Int,
        line: RhymeLine
    ) {
        val positioner = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                    return IntOffset(anchorBounds.right, anchorBounds.bottom)
                }
            }
        }

        Column(modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingClickText(text = line.text, color = MaterialTheme.colorScheme.secondary, onClick = {
                    inputDialog.openSuspend()?.let { text ->
                        val newLyrics = config.lyrics.toMutableList()
                        newLyrics[lineIndex] = line.copy(text = text)
                        rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                    }
                })
                LoadingClickText(text = line.start.toString(), icon = Icons.Outlined.Timer, onClick = {
                    inputDialog.openSuspend()?.let { text ->
                        val newLyrics = config.lyrics.toMutableList()
                        newLyrics[lineIndex] = line.copy(start = text.toLongOrNull() ?: 0L)
                        rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                    }
                })
                ClickIcon(tip = "添加副歌开始", icon = Icons.AutoMirrored.Outlined.AlignHorizontalLeft, color = MaterialTheme.colorScheme.primary, onClick = {
                    val lineStart = line.start
                    val chorus = config.chorus
                    var findIndex = chorus.indexOfFirst { lineStart <= it.start }
                    if (findIndex == -1) findIndex = 0
                    val newChorus = chorus.toMutableList()
                    newChorus.add(findIndex, Chorus(lineStart, lineStart))
                    rhymeConfig = config.copy(chorus = newChorus)
                })
                ClickIcon(tip = "标记副歌结尾", icon = Icons.AutoMirrored.Outlined.AlignHorizontalRight, color = MaterialTheme.colorScheme.primary, onClick = {
                    val lineEnd = line.theme.last().end + line.start
                    val chorus = config.chorus
                    var findIndex = chorus.lastIndex
                    for ((index, v) in chorus.withIndex()) {
                        if (lineEnd < v.start) {
                            findIndex = index - 1
                            break
                        }
                    }
                    if (findIndex in chorus.indices) {
                        val newChorus = chorus.toMutableList()
                        newChorus[findIndex] = newChorus[findIndex].copy(end = lineEnd)
                        rhymeConfig = config.copy(chorus = newChorus)
                    }
                })
                ClickIcon(tip = "删除", icon = Icons.Outlined.Delete, color = MaterialTheme.colorScheme.primary, onClick = {
                    val newLyrics = config.lyrics.toMutableList()
                    newLyrics.removeAt(lineIndex)
                    rhymeConfig = config.copy(lyrics = newLyrics)
                })
                ClickIcon(tip = "复制", icon = Icons.Outlined.ContentCopy, color = MaterialTheme.colorScheme.primary, onClick = {
                    copyData = line.theme
                })
                copyData?.let { data ->
                    ClickIcon(tip = "粘贴", icon = Icons.Outlined.ContentPaste, color = MaterialTheme.colorScheme.primary, onClick = {
                        if (line.theme.size != data.size) slot.tip.warning("粘贴目标与源长度不同")
                        else {
                            val newLyrics = config.lyrics.toMutableList()
                            val newTheme = line.theme.mapIndexed { index, action ->
                                when (val target = data[index]) {
                                    is RhymeAction.Note -> RhymeAction.Note(action.ch, action.end, target.scale)
                                    is RhymeAction.Slur -> RhymeAction.Slur(action.ch, action.end, target.scale)
                                }
                            }
                            newLyrics[lineIndex] = line.copy(theme = newTheme)
                            rhymeConfig = config.copy(lyrics = newLyrics)
                        }
                    })
                }
            }
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                for ((actionIndex, action) in line.theme.withIndex()) {
                    Column(
                        modifier = Modifier.border(width = 1.dp, color = MaterialTheme.colorScheme.tertiary),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = action.ch,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = CustomTheme.padding.verticalSpace)
                        )

                        LoadingClickText(text = action.end.toString(), onClick = {
                            inputDialog.openSuspend()?.let { text ->
                                val newTheme = line.theme.toMutableList()
                                when (action) {
                                    is RhymeAction.Note -> newTheme[actionIndex] = action.copy(end = text.toIntOrNull() ?: 0)
                                    is RhymeAction.Slur -> newTheme[actionIndex] = action.copy(end = text.toIntOrNull() ?: 0)
                                }
                                val newLyrics = config.lyrics.toMutableList()
                                newLyrics[lineIndex] = line.copy(theme = newTheme)
                                rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                            }
                        })

                        val currentScale = remember(action) {
                            when (action) {
                                is RhymeAction.Note -> listOf(action.scale)
                                is RhymeAction.Slur -> action.scale
                            }
                        }
                        val scaleText = remember(action) { currentScale.joinToString("9") { scaleTable[it.toInt()] } }

                        Box {
                            val font = Font(Res.font.music)
                            val fontFamily = remember(font) { FontFamily(font) }
                            var isOpen by rememberFalse()
                            Text(
                                text = scaleText,
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.displayLarge,
                                fontFamily = fontFamily,
                                modifier = Modifier.clickable { isOpen = true }.padding(CustomTheme.padding.value)
                            )

                            if (isOpen) {
                                Popup(
                                    popupPositionProvider = positioner,
                                    onDismissRequest = { isOpen = false }
                                ) {
                                    Surface(
                                        modifier = Modifier.size(200.dp, 150.dp),
                                        shape = MaterialTheme.shapes.extraLarge,
                                        shadowElevation = CustomTheme.shadow.surface
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.equalValue),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                                        ) {
                                            var isAddMode by rememberFalse()

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(text = "添加模式")
                                                Switch(
                                                    checked = isAddMode,
                                                    onCheckedChange = { isAddMode = it },
                                                )
                                            }

                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth().weight(1f),
                                                maxItemsInEachRow = 7,
                                            ) {
                                                repeat(3) { i ->
                                                    repeat(7) { j ->
                                                        val newScaleValue = (i * 7 + j + 1).toByte()
                                                        Text(
                                                            text = scaleTable[newScaleValue.toInt()],
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            style = MaterialTheme.typography.displayMedium,
                                                            textAlign = TextAlign.Center,
                                                            fontFamily = fontFamily,
                                                            modifier = Modifier.weight(1f).clickable {
                                                                val newTheme = line.theme.toMutableList()
                                                                newTheme[actionIndex] = if (isAddMode) RhymeAction.Slur(action.ch, action.end, currentScale.toMutableList().also { it += newScaleValue })
                                                                else RhymeAction.Note(action.ch, action.end, newScaleValue)
                                                                val newLyrics = config.lyrics.toMutableList()
                                                                newLyrics[lineIndex] = line.copy(theme = newTheme)
                                                                rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                                                                isOpen = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
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
    private fun LyricsEditor(modifier: Modifier = Modifier, config: RhymeLyricsConfig) {
        LazyColumn(modifier) {
            itemsIndexed(config.lyrics, key = { _, line -> line.start }) { index, line ->
                LyricsLineEditor(
                    modifier = Modifier.fillMaxWidth().border(CustomTheme.border.small, Colors.Black),
                    config = config,
                    lineIndex = index,
                    line = line,
                )
            }
        }
    }

    @Composable
    private fun ConfigEditor(modifier: Modifier = Modifier, config: RhymeLyricsConfig) {
        Row(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(CustomTheme.padding.equalValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
            ) {
                Text(text = "基础信息", style = MaterialTheme.typography.titleMedium)
                LoadingClickText(text = "ID: ${config.id}", onClick = {
                    inputDialog.openSuspend()?.let { text -> rhymeConfig = config.copy(id = text) }
                })
                LoadingClickText(text = "时长: ${config.duration}", onClick = {
                    inputDialog.openSuspend()?.let { text -> rhymeConfig = config.copy(duration = text.toLongOrNull() ?: 0L) }
                })
                LoadingClickText(text = "偏移: ${config.offset}", onClick = {
                    inputDialog.openSuspend()?.let { text -> rhymeConfig = config.copy(offset = text.toIntOrNull() ?: 0) }
                })

                Text(text = "副歌段", style = MaterialTheme.typography.titleMedium)
                Space()
                config.chorus.fastForEachIndexed { index, chorus ->
                    Text(
                        text = "[${index + 1}] ${chorus.start} -> ${chorus.end}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            launch {
                                if (slot.confirm.openSuspend(content = "删除此条目")) {
                                    val newChorus = config.chorus.toMutableList()
                                    newChorus.removeAt(index)
                                    rhymeConfig = config.copy(chorus = newChorus)
                                }
                            }
                        }.padding(CustomTheme.padding.value)
                    )
                    Space()
                }
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = "歌词",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                )
                LyricsEditor(modifier = Modifier.fillMaxWidth().weight(1f), config = config)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content(device: Device) {
        Row(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                SingleSelector(
                    current = selectedTab,
                    onSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Item("歌词", "歌词")
                    Item("简谱", "简谱")
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedTab) {
                        "歌词" -> {
                            Box(modifier = Modifier.fillMaxSize().dragAndDropTarget(
                                shouldStartDragAndDrop = { event ->
                                    val transferable = event.awtTransferable
                                    transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                                },
                                target = remember { object : DragAndDropTarget {
                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                        val transferable = event.awtTransferable
                                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                            catchingError {
                                                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                                                parseQrc(QrcDecrypter.decrypt((files.first() as File).readBytes())!!)
                                            }?.let { slot.tip.error("解析失败") }
                                            return true
                                        }
                                        return false
                                    }
                                } }
                            )) {
                                ContextMenuDataProvider({
                                    listOf(
                                        ContextMenuItem("全选自动复制") {
                                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(rhymeConfigText), null)
                                            slot.tip.success("复制成功")
                                        }
                                    )
                                }) {
                                    SelectionContainer(modifier = Modifier.fillMaxSize().zIndex(1f).verticalScroll(scrollState)) {
                                        Text(
                                            text = rhymeConfigText,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd).zIndex(2f),
                                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                                )
                            }
                        }
                        "简谱" -> {
                            Box(modifier = Modifier.fillMaxSize().dragAndDropTarget(
                                shouldStartDragAndDrop = { event ->
                                    val transferable = event.awtTransferable
                                    transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                                },
                                target = remember { object : DragAndDropTarget {
                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                        val transferable = event.awtTransferable
                                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                            catching {
                                                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                                                notationImage = (files.first() as File).absolutePath
                                            }
                                            return true
                                        }
                                        return false
                                    }
                                } }
                            )) {
                                notationImage?.let {
                                    ZoomWebImage(
                                        uri = it,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillWidth,
                                        quality = ImageQuality.Full
                                    )
                                } ?: EmptyBox(text = "拖动简谱图片到这")
                            }
                        }
                    }
                }
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            ConfigEditor(Modifier.weight(2f).fillMaxHeight(), rhymeConfig)
        }
    }

    private val inputDialog = this land FloatingDialogInput()
}