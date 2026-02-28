package love.yinlin.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.rememberFontFamily
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.ZoomWebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.LoadingTextButton
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.node.dragAndDrop
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.window.ContextMenuProvider
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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Stable
class ScreenRhyme(private val path: String?) : Screen() {
    private val prettyJson = Json { prettyPrint = true }
    private val defaultConfig = RhymeLyricsConfig(id = "", duration = 0L, chorus = emptyList(), lyrics = emptyList(), offset = 0)

    private val useFile by derivedStateOf { path != null }
    private var name by mutableStateOf("未知歌曲")
    private var rhymeConfig by mutableRefStateOf(defaultConfig)
    private val rhymeConfigText by derivedStateOf { prettyJson.encodeToString(rhymeConfig) }

    private var selectedTab by mutableIntStateOf(0)

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
            if (theme.isNotEmpty()) rhymeLines += RhymeLine(text = lineText.toString(), start = lineStart, theme = theme)
        }

        rhymeConfig = rhymeConfig.copy(lyrics = rhymeLines)
        slot.tip.success("解析成功")
    }

    override val title: String by derivedStateOf { name }

    private suspend fun saveConfig() {
        path?.let {
            if (!slot.confirm.open(content = "替换音游配置到库")) return
            val rhymePath = Path(it, ModResourceType.Rhyme.filename)
            rhymePath.writeText(prettyJson.encodeToString(rhymeConfig))
            slot.tip.success("保存成功")
        }
    }

    private suspend fun deleteConfig() {
        path?.let {
            if (!slot.confirm.open(content = "删除音游配置")) return
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
    override fun RowScope.RightActions() {
        LoadingIcon(Icons.Check, enabled = useFile, onClick = ::saveConfig)
        LoadingIcon(Icons.Delete, enabled = useFile, onClick = ::deleteConfig)
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
        Column(modifier) {
            ActionScope.Left.Container(modifier = Modifier.fillMaxWidth()) {
                LoadingTextButton(text = line.text, color = Theme.color.secondary, onClick = {
                    inputDialog.open()?.let { text ->
                        val newLyrics = config.lyrics.toMutableList()
                        newLyrics[lineIndex] = line.copy(text = text)
                        rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                    }
                })
                LoadingTextButton(text = line.start.toString(), icon = Icons.Timer, onClick = {
                    inputDialog.open()?.let { text ->
                        val newLyrics = config.lyrics.toMutableList()
                        newLyrics[lineIndex] = line.copy(start = text.toLongOrNull() ?: 0L)
                        rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                    }
                })
                Icon(tip = "添加副歌开始", icon = Icons.AlignHorizontalLeft, color = Theme.color.primary, onClick = {
                    val lineStart = line.start
                    val chorus = config.chorus
                    var findIndex = chorus.indexOfFirst { lineStart <= it.start }
                    if (findIndex == -1) findIndex = 0
                    val newChorus = chorus.toMutableList()
                    newChorus.add(findIndex, Chorus(lineStart, lineStart))
                    rhymeConfig = config.copy(chorus = newChorus)
                })
                Icon(tip = "标记副歌结尾", icon = Icons.AlignHorizontalRight, color = Theme.color.primary, onClick = {
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
                Icon(tip = "删除", icon = Icons.Delete, color = Theme.color.primary, onClick = {
                    val newLyrics = config.lyrics.toMutableList()
                    newLyrics.removeAt(lineIndex)
                    rhymeConfig = config.copy(lyrics = newLyrics)
                })
                Icon(tip = "复制", icon = Icons.ContentCopy, color = Theme.color.primary, onClick = {
                    copyData = line.theme
                })
                copyData?.let { data ->
                    Icon(tip = "粘贴", icon = Icons.ContentPaste, color = Theme.color.primary, onClick = {
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
                        modifier = Modifier.border(width = Theme.border.v7, color = Theme.color.tertiary),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = action.ch, style = Theme.typography.v6.bold)

                        LoadingTextButton(text = action.end.toString(), onClick = {
                            inputDialog.open()?.let { text ->
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

                        val fontFamily = rememberFontFamily(Res.font.music)
                        var isOpen by rememberFalse()

                        val currentScale = when (action) {
                            is RhymeAction.Note -> listOf(action.scale)
                            is RhymeAction.Slur -> action.scale
                        }

                        Flyout(
                            visible = isOpen,
                            onClickOutside = { isOpen = false },
                            position = FlyoutPosition.Bottom,
                            clip = true,
                            flyout = {
                                Surface(
                                    contentPadding = Theme.padding.eValue,
                                    shape = Theme.shape.v1,
                                    shadowElevation = Theme.shadow.v3
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                        var isAddMode by rememberFalse()

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Switch(checked = isAddMode, onCheckedChange = { isAddMode = it })
                                            Text(text = "添加模式")
                                        }

                                        FlowRow(maxItemsInEachRow = 7, maxLines = 3) {
                                            repeat(21) { index ->
                                                val newScaleValue = (index / 7 * 7 + index % 7 + 1).toByte()
                                                Text(
                                                    text = scaleTable[newScaleValue.toInt()],
                                                    style = Theme.typography.v3.bold,
                                                    fontFamily = fontFamily,
                                                    modifier = Modifier.clickable {
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
                        ) {
                            Text(
                                text = currentScale.joinToString("9") { scaleTable[it.toInt()] },
                                color = Theme.color.tertiary,
                                style = Theme.typography.v3.bold,
                                fontFamily = fontFamily,
                                modifier = Modifier.clickable { isOpen = true }.padding(Theme.padding.value)
                            )
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
                    modifier = Modifier.fillMaxWidth().border(Theme.border.v7, Colors.Black),
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
                modifier = Modifier.fillMaxHeight().padding(Theme.padding.eValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            ) {
                Text(text = "基础信息", style = Theme.typography.v7.bold)
                LoadingTextButton(text = "ID: ${config.id}", onClick = {
                    inputDialog.open()?.let { text -> rhymeConfig = config.copy(id = text) }
                })
                LoadingTextButton(text = "时长: ${config.duration}", onClick = {
                    inputDialog.open()?.let { text -> rhymeConfig = config.copy(duration = text.toLongOrNull() ?: 0L) }
                })
                LoadingTextButton(text = "偏移: ${config.offset}", onClick = {
                    inputDialog.open()?.let { text -> rhymeConfig = config.copy(offset = text.toIntOrNull() ?: 0) }
                })

                Text(text = "副歌段", style = Theme.typography.v7.bold)
                Space()
                config.chorus.fastForEachIndexed { index, chorus ->
                    Text(
                        text = "[${index + 1}] ${chorus.start} -> ${chorus.end}",
                        color = Theme.color.primary,
                        modifier = Modifier.clickable {
                            launch {
                                if (slot.confirm.open(content = "删除此条目")) {
                                    val newChorus = config.chorus.toMutableList()
                                    newChorus.removeAt(index)
                                    rhymeConfig = config.copy(chorus = newChorus)
                                }
                            }
                        }.padding(Theme.padding.value)
                    )
                    Space()
                }
            }
            Divider()
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = "歌词",
                    style = Theme.typography.v7.bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue)
                )
                LyricsEditor(modifier = Modifier.fillMaxWidth().weight(1f), config = config)
            }
        }
    }

    @Composable
    override fun Content() {
        Row(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val tabs = remember { listOf("歌词", "简谱") }

                Filter(
                    size = tabs.size,
                    selectedProvider = { it == selectedTab },
                    titleProvider = { tabs[it] },
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                    onClick = { index, checked -> if (checked) selectedTab = index }
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            Box(modifier = Modifier.fillMaxSize().dragAndDrop(true, DragFlag.File) { dropResult ->
                                catchingError {
                                    launch {
                                        parseQrc(QrcDecrypter.decrypt((dropResult as DropResult.File).path.first().readByteArray()!!)!!)
                                    }
                                }?.let { slot.tip.error("解析失败") }
                            }) {
                                ContextMenuProvider({
                                    item("全选自动复制") {
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(rhymeConfigText), null)
                                        slot.tip.success("复制成功")
                                    }
                                }) {
                                    SelectionBox {
                                        Text(
                                            text = rhymeConfigText,
                                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                                        )
                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd).zIndex(2f),
                                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                                )
                            }
                        }
                        1 -> {
                            Box(modifier = Modifier.fillMaxSize().dragAndDrop(true, DragFlag.File) { dropResult ->
                                catching { notationImage = (dropResult as DropResult.File).path.first().toString() }
                            }) {
                                val showImage = notationImage
                                if (showImage != null) {
                                    ZoomWebImage(
                                        uri = showImage,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillWidth,
                                        quality = ImageQuality.Full
                                    )
                                }
                                else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(Theme.padding.value).dashBorder(Theme.border.v6, Theme.color.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "拖动简谱图片到这")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Divider()
            ConfigEditor(Modifier.weight(2f).fillMaxHeight(), rhymeConfig)
        }
    }

    private val inputDialog = this land DialogInput()
}