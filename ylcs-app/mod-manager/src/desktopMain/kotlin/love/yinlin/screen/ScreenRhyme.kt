package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.Chorus
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLine
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.*
import love.yinlin.platform.Coroutines

@Stable
class ScreenRhyme(manager: ScreenManager, private val path: String?) : Screen(manager) {
    private val useFile by derivedStateOf { path != null }
    private var name by mutableStateOf("未知歌曲")
    private val rhymeConfig = TextInputState()
    private val platformConfig = TextInputState()

    private val prettyJson = Json { prettyPrint = true }

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

        rhymeConfig.text = prettyJson.encodeToString(rhymeConfig.text.parseJsonValue<RhymeLyricsConfig>().copy(lyrics = rhymeLines))
        slot.tip.success("解析成功")
    }

    override val title: String by derivedStateOf { name }

    private suspend fun updateConfig() {
        val text = platformConfig.text.trim()
        Coroutines.cpu {
            catchingError {
                if (text.isEmpty()) slot.tip.warning("平台歌词为空")
                if (text.contains("QrcInfos")) parseQrc(text)
                else slot.tip.warning("不支持的平台歌词")
            }.errorTip
        }
    }

    private suspend fun saveConfig() {
        path?.let {
            if (!slot.confirm.openSuspend(content = "替换音游配置到库")) return
            val rhymePath = Path(it, ModResourceType.Rhyme.filename)
            rhymePath.writeText(rhymeConfig.text)
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
            rhymeConfig.text = Coroutines.io {
                path?.let {
                    val musicInfo = Path(it, ModResourceType.Config.filename).readText()!!.parseJsonValue<MusicInfo>()
                    val rhymePath = Path(it, ModResourceType.Rhyme.filename)
                    val newConfig = RhymeLyricsConfig(
                        id = musicInfo.id,
                        duration = 0L,
                        chorus = musicInfo.chorus?.map { Chorus(it, it) } ?: emptyList(),
                        lyrics = emptyList(),
                        offset = 0,
                    )
                    if (!rhymePath.exists) rhymePath.writeText(prettyJson.encodeToString(newConfig))
                    name = musicInfo.name
                    rhymePath.readText()!!
                } ?: run {
                    name = "未知歌曲"
                    prettyJson.encodeToString(RhymeLyricsConfig(
                        id = "",
                        duration = 0L,
                        chorus = emptyList(),
                        lyrics = emptyList(),
                        offset = 0,
                    ))
                }
            }
        }.errorTip
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.TurnLeft, onClick = ::updateConfig)
        ActionSuspend(Icons.Outlined.Done, enabled = useFile, onClick = ::saveConfig)
        ActionSuspend(Icons.Outlined.Delete, enabled = useFile, onClick = ::deleteConfig)
    }

    @Composable
    override fun Content(device: Device) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(CustomTheme.padding.equalValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(text = "音游配置")
                TextInput(
                    state = rhymeConfig,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = Int.MAX_VALUE,
                    clearButton = false
                )
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight().zIndex(1f),
                thickness = 10.dp,
                color = Colors.White
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(CustomTheme.padding.equalValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(text = "平台歌词")
                TextInput(
                    state = platformConfig,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = Int.MAX_VALUE,
                    clearButton = false
                )
            }
        }
    }
}