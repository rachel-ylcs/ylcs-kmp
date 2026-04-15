package love.yinlin.compose.game.plugin

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.DataUpdater
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.data.RhymePlayResult
import love.yinlin.compose.game.layer.BackgroundLayer
import love.yinlin.compose.game.layer.InteractLayer
import love.yinlin.compose.game.layer.MapLayer
import love.yinlin.compose.game.ui.RhymeBlurSurface
import love.yinlin.compose.game.ui.RhymeCommonButton
import love.yinlin.compose.game.ui.rhymeBlurTarget
import love.yinlin.compose.rememberFontFamily
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.RhymeIcons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContext
import love.yinlin.fs.File
import love.yinlin.media.buildAudioPlayer
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

@Stable
class RhymePlugin(
    context: PlatformContext,
    private val blurState: BlurState,
    private val endListener: (RhymePlayResult?) -> Unit,
    engine: Engine,
) : UIPlugin(engine) {
    @Stable
    class Factory(
        private val context: PlatformContext,
        private val blurState: BlurState,
        private val endListener: (RhymePlayResult?) -> Unit,
    ) : PluginFactory {
        override fun build(engine: Engine): Plugin = RhymePlugin(context, blurState, endListener, engine)
    }

    override val dependencies: List<KClass<out Plugin>> = listOf(ScenePlugin::class, SoundPlugin::class)

    private lateinit var scene: ScenePlugin

    private val player = buildAudioPlayer(context) {

    }

    private var currentInfo: RhymePlayInfo? = null

    private val dataUpdater = DataUpdater()

    // 初始化游戏
    suspend fun setupGame(playInfo: RhymePlayInfo, audio: File) {
        currentInfo = playInfo
        player.load(audio, true)
        dataUpdater.init(playInfo)
        val backgroundLayer = BackgroundLayer()
        val interactLayer = InteractLayer()
        val mapLayer = MapLayer(scene.camera, player, playInfo, dataUpdater, interactLayer)
        // 先更新交互结果再处理地图
        scene += listOf(backgroundLayer, interactLayer, mapLayer)
    }

    // 停止游戏
    fun stopGame() {
        scene.reset()
        endListener(null)
        player.stop()
        currentInfo = null
        dataUpdater.reset()
    }

    override suspend fun onInitialize(): Boolean {
        scene = engine.plugin()
        Coroutines.main { player.init() }
        return true
    }

    override fun onRelease() {
        stopGame()
        player.release()
    }

    @Composable
    private fun PauseContent(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            RhymeBlurSurface(
                modifier = Modifier.width(Theme.size.cell1),
                blurState = blurState,
                shape = Theme.shape.v3,
                border = Theme.border.v7,
                contentPadding = Theme.padding.value5
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9, Alignment.CenterVertically)
                ) {
                    SimpleClipText(text = "暂停中", style = Theme.typography.v5.bold, modifier = Modifier.padding(Theme.padding.v9))
                    RhymeCommonButton(icon = Icons.Clear, text = "退出", onClick = ::stopGame, modifier = Modifier.fillMaxWidth())
                    RhymeCommonButton(icon = Icons.PlayArrow, text = "继续", onClick = { engine.isRunning = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    @Composable
    private fun InfoContent() {
        currentInfo?.let { info ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).rhymeBlurTarget(blurState).padding(Theme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
            ) {
                // 画封面
                Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f).silentClick {
                    engine.isRunning = false
                }.drawWithContent {
                    // 画背景
                    val bounds = Rect(0f, 0f, size.width, size.height)
                    val strokeWidth = size.width / 20
                    clipPath(Path().apply { addOval(bounds) }) {
                        drawImage(
                            image = info.musicRecord,
                            dstOffset = IntOffset.Zero,
                            dstSize = size.toIntSize()
                        )
                    }
                    // 画时长
                    drawArc(
                        color = Colors.White,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    // 画进度
                    drawArc(
                        color = Colors.Green4,
                        startAngle = -90f,
                        sweepAngle = 360f * dataUpdater.audioProgress,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                })
                Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                    // 画标题
                    SimpleClipText(
                        text = info.musicInfo.name,
                        style = Theme.typography.v6.bold
                    )
                    // 画难度星级
                    Row {
                       repeat(info.playConfig.difficulty.ordinal + 1) {
                           Icon(icon = RhymeIcons.Star, color = Colors.Unspecified)
                       }
                    }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.ResultContent() {
        val fontFamily = rememberFontFamily(Res.font.rhyme)

        val (result, combo, id) = dataUpdater.currentResult ?: return

        val scale = remember { Animatable(0f) }

        LaunchedEffect(id) {
            scale.snapTo(targetValue = 0f)
            scale.animateTo(targetValue = 1f)
            delay(1.seconds)
            scale.animateTo(targetValue = 0f)
        }

        Box(modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin.Center
        }.align(Alignment.CenterHorizontally)) {
            SimpleClipText(
                text = result.title,
                color = Colors.White,
                style = Theme.typography.v3.bold.copy(fontFamily = fontFamily)
            )
            if (combo > 0) {
                SimpleClipText(
                    text = "+$combo",
                    color = Colors.White,
                    style = Theme.typography.v6.bold.copy(fontFamily = fontFamily),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }

    @Composable
    private fun UIContent(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
        ) {
            InfoContent()
            ResultContent()
        }
    }

    @Composable
    override fun BoxScope.Content() {
        UIContent(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current).zIndex(1f))
        if (!engine.isRunning) PauseContent(modifier = Modifier.fillMaxSize().background(Theme.color.scrim.copy(alpha = 0.6f)).silentClick { }.zIndex(2f))

        LaunchedEffect(engine.isRunning) {
            if (engine.isRunning) player.play()
            else player.pause()
        }
    }
}