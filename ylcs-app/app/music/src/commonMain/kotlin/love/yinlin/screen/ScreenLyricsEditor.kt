package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.*
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.OverlayAction
import love.yinlin.compose.ui.container.OverlayTopBar
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.text.FastFixedText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.timeString
import love.yinlin.media.buildAudioPlayer
import love.yinlin.tpl.lyrics.LrcLine
import love.yinlin.tpl.lyrics.LrcParser
import kotlin.time.Duration.Companion.milliseconds

@Stable
class ScreenLyricsEditor(private val musicInfo: MusicInfo) : BasicScreen() {
    private val player = buildAudioPlayer(app.rawContext) {
        launch { loadMusic(true) }
    }

    private var isPlaying by mutableStateOf(false)
    private var position by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)
    private val isPlayingFlow = MutableStateFlow(false)

    private val lyrics = mutableStateListOf<UUIDKey<LrcLine>>()
    private var currentIndex by mutableIntStateOf(-1)
    private var lastPosition: Long = 0L
    private val canSave by derivedStateOf { lyrics.size >= 5 }

    private val listState = LazyListState()

    override suspend fun initialize() {
        player.init()

        catchingError {
            require(player.isInit)
            loadMusic(false)

            val parser = LrcParser(musicInfo.path(app.modPath, ModResourceType.LineLyrics).readText()!!)
            lyrics.replaceAllByData(parser.lines!!)

            launch {
                isPlayingFlow.collectLatest { value ->
                    isPlaying = value
                    duration = player.duration
                    if (value) {
                        while (isActive) {
                            position = player.position
                            updatePosition(position)
                            delay(32.milliseconds)
                        }
                    }
                }
            }

        }?.let { slot.tip.error("播放器加载失败") }
    }

    override fun finalize() {
        player.release()
    }

    private suspend fun loadMusic(playing: Boolean) {
        player.load(musicInfo.path(app.modPath, ModResourceType.Audio), playing)
    }

    private fun updatePosition(newPosition: Long) {
        // 先检查是否位于当前句和下一句之间, 此分支命中率最高
        if (newPosition >= lastPosition) {
            val nextPosition1 = lyrics.getOrNullByData(currentIndex + 1)?.position ?: Long.MAX_VALUE
            val nextPosition2 = lyrics.getOrNullByData(currentIndex + 2)?.position ?: Long.MAX_VALUE
            lastPosition = newPosition
            if (newPosition < nextPosition1) return // 不需要更新
            else if (newPosition < nextPosition2) { // 前移
                ++currentIndex
                return
            }
        }
        else lastPosition = newPosition
        val targetIndex = lyrics.indexOfFirstByData { it.position > newPosition }
        currentIndex = if (targetIndex == -1) {
            if (currentIndex != lyrics.lastIndex) lyrics.lastIndex
            else return // 到达最后一句不需要更新
        }
        else if (targetIndex > 0) targetIndex - 1
        else -1
    }

    private suspend fun saveLyrics() {
        catchingError {
            Coroutines.io {
                val parser = LrcParser(lyrics.data)
                musicInfo.path(app.modPath, ModResourceType.LineLyrics).writeText(parser.toString())
            }
            slot.tip.success("保存成功")
        }.errorTip
    }

    @Composable
    private fun MusicProgressLayout(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon = if (isPlaying) Icons.Pause else Icons.Play,
                onClick = {
                    if (isPlaying) player.pause()
                    else player.play()
                    isPlayingFlow.value = !isPlaying
                }
            )
            FastFixedText("00:00", { position.timeString })
            Slider(
                value = if (duration == 0L) 0f else position / duration.toFloat(),
                onValueChangeFinished = { newProgress ->
                    launch {
                        player.seekTo((newProgress * duration).toLong())
                        isPlayingFlow.value = true
                    }
                },
                enabled = duration != 0L,
                trackHeight = Theme.size.box4,
                trackColor = Colors.Gray3,
                activeColor = Colors.Green5,
                trackShape = Theme.shape.circle,
                showThumb = false,
                modifier = Modifier.weight(1f)
            )
            FastFixedText("00:00", { duration.timeString })
        }
    }

    @Composable
    private fun LyricsEditorLayout(modifier: Modifier = Modifier) {
        LazyColumn(
            modifier = modifier,
            state = listState
        ) {
            itemsIndexed(
                items = lyrics,
                key = { _, item -> item.key }
            ) { index, (item) ->
                val isCurrent = currentIndex == index
                val backgroundColor = if (isCurrent) Theme.color.primaryContainer.copy(alpha = 0.5f) else Theme.color.background
                val contentColor = if (isCurrent) Theme.color.onContainer else Theme.color.onBackground

                ThemeContainer(contentColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).background(backgroundColor).clickable { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f).clickable {
                                lyrics.setByData(index) { it.copy(position = position) }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon = Icons.Flag)
                        }

                        Column(
                            modifier = Modifier.weight(1f).clickable {
                                launch {
                                    val needPlay = player.isPlaying
                                    if (needPlay) player.pause()
                                    inputDialog.open(initText = item.text)?.let { text ->
                                        lyrics.setByData(index) { it.copy(text = text) }
                                    }
                                    if (needPlay) player.play()
                                }
                            }.padding(vertical = Theme.padding.v),
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                        ) {
                            SimpleEllipsisText(
                                text = "${item.position.timeString}.${(item.position % 1000).toString().padStart(3, '0')}",
                                color = LocalColorVariant.current
                            )
                            SimpleEllipsisText(
                                text = item.text
                            )
                        }

                        ActionScope.Right.Container(
                            modifier = Modifier.padding(Theme.padding.eValue),
                            padding = Theme.padding.e
                        ) {
                            Icon(icon = Icons.Add, onClick = {
                                lyrics.addByData(index, LrcLine(position, "点击输入歌词行"))
                            })
                            Icon(icon = Icons.Delete, onClick = {
                                lyrics.removeAt(index)
                            })
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            OverlayTopBar(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                left = OverlayAction.Sync("返回", Icons.ArrowBack, onClick = ::onBack),
                right = OverlayAction.Async("保存", Icons.Check, canSave, onClick = ::saveLyrics)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocalFileImage(
                    uri = musicInfo.path(app.modPath, ModResourceType.Record).path,
                    circle = true,
                    modifier = Modifier.size(Theme.size.image7)
                )
                SimpleEllipsisText(text = musicInfo.name, style = Theme.typography.v6.bold)
            }
            ActionScope.Left.Container(Modifier.fillMaxWidth().padding(Theme.padding.value9)) {
                PrimaryLoadingButton(text = "滚动到当前", onClick = {
                    if (currentIndex in lyrics.indices) listState.animateScrollToItem(currentIndex)
                })
                PrimaryLoadingButton(text = "全局偏移", onClick = {
                    val offset = catchingNull { offsetDialog.open("0")?.toLong() ?: 0L }
                    if (offset == null) slot.tip.warning("偏移不是整数")
                    else lyrics.replaceAllByData(lyrics.mapByData { it.copy(position = it.position + offset) })
                })
            }
            MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value))
            LyricsEditorLayout(modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }

    private val inputDialog = this land DialogInput(hint = "输入歌词行", maxLength = 32)
    private val offsetDialog = this land DialogInput(hint = "输入毫秒偏移量(-9000 ~ 9000)", maxLength = 5)
}