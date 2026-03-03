package love.yinlin.lyricseditor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.LoadingTextButton
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.music.Chorus
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLine
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingError
import love.yinlin.extension.parseJsonValue

@Stable
class ScreenMain : BasicScreen() {
    val assetManager = app.context.activity.assets!!

    @Stable
    @Serializable
    data class Song(val sid: String, val name: String, val duration: Long)

    var songs by mutableStateOf(emptyList<Song>())
    var currentSong: Song? by mutableStateOf(null)

    val prettyJson = Json { prettyPrint = true }
    val defaultConfig = RhymeLyricsConfig(id = "", duration = 0L, chorus = emptyList(), lyrics = emptyList(), offset = 0)
    var rhymeConfig by mutableRefStateOf(defaultConfig)
    val rhymeConfigText by derivedStateOf { prettyJson.encodeToString(rhymeConfig) }
    var copyData by mutableRefStateOf<List<RhymeAction>?>(null)

    override suspend fun initialize() {
        songs = catchingDefault(emptyList()) {
            Coroutines.io {
                assetManager.open("song.json").use { it.readBytes() }.decodeToString().parseJsonValue<List<Song>>()
            }
        }
    }

    fun parseQrc(qrc: String): List<RhymeLine> {
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
                if (ch.length != 1 || ch.isBlank()) continue
                lineText.append(ch)
                theme += RhymeAction.Note(
                    ch = ch,
                    end = (match.groupValues[2].toLong() + match.groupValues[3].toLong() - lineStart).toInt(),
                    scale = 1
                )
            }
            if (theme.isNotEmpty()) rhymeLines += RhymeLine(text = lineText.toString(), start = lineStart, theme = theme)
        }
        return rhymeLines
    }

    suspend fun searchSong(name: String) {
        val targetSongs = songs.filter { it.name.contains(name, true) }
        if (targetSongs.isNotEmpty()) {
            val index = choiceDialog.openSuspend(targetSongs.map { "${it.name} - ${it.sid}" })
            if (index != null) {
                catchingError {
                    val song = targetSongs[index]
                    currentSong = song

                    rhymeConfig = Coroutines.io {
                        val data = assetManager.open("${song.sid}.qrc").use { it.readBytes() }
                        RhymeLyricsConfig(
                            id = song.sid,
                            duration = song.duration,
                            chorus = emptyList(),
                            offset = 0,
                            lyrics = parseQrc(QrcDecrypter.decrypt(data)!!)
                        )
                    }
                }?.let { slot.tip.error("歌曲解析错误") }
            }
        }
        else slot.tip.warning("未搜索到相关歌曲")
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
            LoadingTextButton(text = line.text, color = Theme.color.secondary, style = Theme.typography.v6.bold, onClick = {
                inputDialog.open()?.let { text ->
                    val newLyrics = config.lyrics.toMutableList()
                    newLyrics[lineIndex] = line.copy(text = text)
                    rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                }
            })
            ActionScope.Left.Container(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)) {
                LoadingTextButton(text = "行开始: ${line.start}", icon = Icons.Timer, onClick = {
                    inputDialog.open()?.let { text ->
                        val newLyrics = config.lyrics.toMutableList()
                        newLyrics[lineIndex] = line.copy(start = text.toLongOrNull() ?: 0L)
                        rhymeConfig = rhymeConfig.copy(lyrics = newLyrics)
                    }
                })
                Icon(tip = "添加副歌开始", icon = Icons.AlignHorizontalLeft, color = Theme.color.primary, onClick = {
                    val lineStart = line.start
                    val chorus = config.chorus
                    var findIndex = chorus.binarySearchBy(lineStart) { it.start }
                    if (findIndex < 0) findIndex = -(findIndex + 1)
                    val newChorus = chorus.toMutableList()
                    newChorus.add(findIndex, Chorus(lineStart, lineStart))
                    rhymeConfig = config.copy(chorus = newChorus)
                })
                Icon(tip = "标记副歌结尾", icon = Icons.AlignHorizontalRight, color = Theme.color.primary, onClick = {
                    val lineEnd = line.theme.last().end + line.start
                    val chorus = config.chorus
                    if (chorus.isNotEmpty()) {
                        val findIndex = chorus.indexOfFirst { it.start > lineEnd }
                        val index = when {
                            findIndex == -1 -> chorus.lastIndex
                            findIndex > 0 -> findIndex - 1
                            else -> null
                        }
                        if (index != null) {
                            val newChorus = chorus.toMutableList()
                            newChorus[index] = newChorus[index].copy(end = lineEnd)
                            rhymeConfig = config.copy(chorus = newChorus)
                        }
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
                        modifier = Modifier.border(width = Theme.border.v7, color = Theme.color.tertiary).padding(horizontal = Theme.padding.h),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = action.ch, style = Theme.typography.v6.bold)

                        Text(text = action.end.toString(), modifier = Modifier.clickable {
                            launch {
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
                            }
                        })

                        val fontFamily = remember { FontFamily(Font(R.font.music)) }
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

                                        FlowRow(
                                            maxItemsInEachRow = 7,
                                            maxLines = 3,
                                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                                        ) {
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
                                modifier = Modifier.clickable { isOpen = true }
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
    override fun BasicContent() {
        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val state = rememberInputState()

                Input(state = state, hint = "模糊搜索歌名", modifier = Modifier.weight(1f))
                PrimaryLoadingButton(text = "搜索", icon = Icons.Search, enabled = state.isSafe, onClick = {
                    searchSong(state.text)
                })
                SecondaryButton(text = "导出", icon = Icons.Upload, onClick = {
                    val clipboard = app.context.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("", rhymeConfigText)
                    clipboard.setPrimaryClip(clip)
                    slot.tip.success("已复制到剪贴板")
                })
            }
            LyricsEditor(modifier = Modifier.fillMaxWidth().weight(1f), rhymeConfig)
            Column(
                modifier = Modifier.fillMaxWidth().border(Theme.border.v7, Theme.color.tertiary).padding(Theme.padding.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SimpleEllipsisText(text = "ID: ${currentSong?.sid ?: "空"}")
                    SimpleEllipsisText(text = "歌名: ${currentSong?.name ?: "空"}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SimpleEllipsisText(text = "偏移: ${rhymeConfig.offset}")
                    SimpleEllipsisText(text = "时长: ${rhymeConfig.duration}")
                }
                SimpleEllipsisText(text = "副歌段(长按删除)")
                TagView(
                    size = rhymeConfig.chorus.size,
                    titleProvider = {
                        val chorus = rhymeConfig.chorus[it]
                        "${chorus.start} - ${chorus.end}"
                    },
                    onDelete = { index ->
                        val newChorus = rhymeConfig.chorus.toMutableList()
                        newChorus.removeAt(index)
                        rhymeConfig = rhymeConfig.copy(chorus = newChorus)
                    },
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f),
                )
            }
        }
    }

    val choiceDialog = this land DialogChoice.ByDynamicList()
    val inputDialog = this land DialogInput()
}