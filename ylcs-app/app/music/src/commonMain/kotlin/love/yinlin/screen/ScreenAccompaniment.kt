package love.yinlin.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import love.yinlin.app
import love.yinlin.app.music.resources.Res
import love.yinlin.app.music.resources.img_music_record
import love.yinlin.common.PathMod
import love.yinlin.compose.ColorSystem
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberRefState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.rememberOffScreenState
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.blurSource
import love.yinlin.compose.ui.node.blurTarget
import love.yinlin.compose.ui.node.fastClipCircle
import love.yinlin.compose.ui.node.fastRotate
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.catchingError
import love.yinlin.extension.timeString
import love.yinlin.media.buildAudioPlayer
import love.yinlin.media.lyrics.LyricsEngine
import love.yinlin.media.lyrics.LyricsEngineHost
import love.yinlin.media.lyrics.LyricsEngineType
import kotlin.time.Duration.Companion.milliseconds

@Stable
class ScreenAccompaniment(private val music: MusicInfo, engineType: LyricsEngineType) : BasicScreen() {
    private val player = buildAudioPlayer(app.context) { pop() }
    private val engine = LyricsEngine.clone(engineType)
    private val engineHost = LyricsEngineHost {
        player.seekTo(it)
        isPlayingFlow.value = true
    }

    private var isPlaying by mutableStateOf(false)
    private var position by mutableStateOf(0L)
    private var duration by mutableStateOf(0L)

    private val isPlayingFlow = MutableStateFlow(false)

    private val blurState = BlurState()

    override suspend fun initialize() {
        player.init()

        catchingError {
            require(player.isInit)
            player.load(music.path(PathMod, ModResourceType.Accompaniment), false)

            Coroutines.io {
                engine.load(music.path(PathMod))
            }

            launch {
                isPlayingFlow.collectLatest { value ->
                    isPlaying = value
                    duration = player.duration
                    if (value) {
                        while (isActive) {
                            val newPosition = player.position
                            position = newPosition
                            engine.update(newPosition)
                            delay(engine.interval.milliseconds)
                        }
                    }
                }
            }
        }?.let { slot.tip.error("播放器加载失败") }
    }

    override fun finalize() {
        player.release()
    }

    @Composable
    private fun MusicCover(modifier: Modifier = Modifier) {
        var animationRecord by rememberRefState { Animatable(0f) }
        var lastDegree by rememberValueState(0f)
        val isForeground = rememberOffScreenState()

        LaunchedEffect(isPlaying, isForeground) {
            if (isPlaying && isForeground) {
                animationRecord.animateTo(
                    targetValue = 360f + lastDegree,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 15000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                ) {
                    lastDegree = this.value
                }
            }
            else {
                animationRecord.snapTo(lastDegree)
                animationRecord.stop()
            }
        }

        LocalFileImage(
            uri = music.path(PathMod, ModResourceType.Record).toString(),
            contentScale = ContentScale.Crop,
            modifier = modifier.fastRotate(animationRecord)
        )
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
            SimpleClipText(text = position.timeString)
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
            SimpleClipText(text = duration.timeString)
        }
    }

    @Composable
    override fun BasicContent() {
        Box(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().background(ColorSystem.Default.dark.background),
            contentAlignment = Alignment.Center
        ) {
            ThemeContainer(ColorSystem.Default.dark.onBackground, ColorSystem.Default.dark.onBackgroundVariant) {
                val maxWidth = Theme.size.cell1 * 1.25f

                LocalFileImage(
                    uri = music.path(PathMod, ModResourceType.Background).toString(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.85f,
                    modifier = Modifier.widthIn(max = maxWidth).fillMaxSize().blurSource(blurState).zIndex(1f)
                )

                Column(
                    modifier = Modifier.widthIn(max = maxWidth).fillMaxSize().zIndex(2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(Theme.size.image2).shadow(Theme.shape.circle, Theme.shadow.v3),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(res = Res.drawable.img_music_record, modifier = Modifier.fillMaxSize().zIndex(1f))
                        MusicCover(modifier = Modifier.fillMaxSize(fraction = 0.641f).fastClipCircle().border(
                            width = Theme.border.v10,
                            color = Theme.color.outline,
                            shape = Theme.shape.circle
                        ).zIndex(2f))
                    }

                    Column(
                        modifier = Modifier.padding(Theme.padding.eValue9).fillMaxWidth().clip(Theme.shape.v5).blurTarget(blurState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SimpleClipText(text = music.name, color = Theme.color.primary, style = Theme.typography.v5.bold, modifier = Modifier.padding(Theme.padding.value))
                        MusicProgressLayout(modifier = Modifier.padding(horizontal = Theme.padding.h).fillMaxWidth())
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
                            engine.LyricsCanvas(config = app.config.lyricsEngineConfig, host = engineHost)
                        }
                    }
                }
            }
        }
    }
}