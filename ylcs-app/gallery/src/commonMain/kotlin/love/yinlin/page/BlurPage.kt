package love.yinlin.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberRefState
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.input.ColorPicker
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.SliderDpConverter
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.node.BlurParameters
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.blurSource
import love.yinlin.compose.ui.node.blurTarget
import love.yinlin.compose.ui.text.Text
import kotlin.math.roundToInt

@Stable
object BlurPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Source / Target") {
                ParametersExample()
            }
            Component("多个 Target") {
                MultipleTargetsExample()
            }
            Component("动态与重挂载") {
                LifecycleExample()
            }
        }
    }

    @Composable
    private fun ParametersExample() {
        val state = remember { BlurState() }
        var parameters by rememberState {
            BlurParameters(
                blurRadius = 10.dp,
                backgroundColor = Color(0xFF292929),
                tintColor = Color(0x8C292929),
                noiseFactor = 0.15f,
                qualityFraction = 0.66667f,
            )
        }
        var paletteVersion by rememberValueState(0)

        GalleryActions {
            PrimaryTextButton("播放页参数", onClick = {
                parameters = BlurParameters(
                    blurRadius = 10.dp,
                    backgroundColor = Color(0xFF292929),
                    tintColor = Color(0x8C292929),
                    noiseFactor = 0.15f,
                    qualityFraction = 0.66667f,
                )
                paletteVersion++
            })
            PrimaryTextButton("暂停面板参数", onClick = {
                parameters = BlurParameters(
                    blurRadius = 10.dp,
                    backgroundColor = Color(0xDD292929),
                    tintColor = Color(0x6C292929),
                    noiseFactor = 0.15f,
                    qualityFraction = 0.66667f,
                )
                paletteVersion++
            })
            PrimaryTextButton("零半径 / 无噪点", onClick = {
                parameters = parameters.copy(
                    blurRadius = 0.dp,
                    noiseFactor = 0f,
                    backgroundColor = Color.Transparent,
                    tintColor = Color.Transparent,
                )
                paletteVersion++
            })
        }

        ExampleRow(maxItemsInEachRow = 3) {
            Example("半径 · ${parameters.blurRadius.value.roundToInt()} dp", Modifier.weight(1f)) {
                Slider(
                    value = parameters.blurRadius,
                    converter = SliderDpConverter(0.dp, 80.dp),
                    onValueChangeFinished = { parameters = parameters.copy(blurRadius = it) },
                    onValueChange = { parameters = parameters.copy(blurRadius = it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Example("质量 · ${(parameters.qualityFraction * 100).roundToInt()}%", Modifier.weight(1f)) {
                Slider(
                    value = parameters.qualityFraction,
                    onValueChangeFinished = { parameters = parameters.copy(qualityFraction = it) },
                    onValueChange = { parameters = parameters.copy(qualityFraction = it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Example("噪点 · ${(parameters.noiseFactor * 100).roundToInt()}%", Modifier.weight(1f)) {
                Slider(
                    value = parameters.noiseFactor,
                    onValueChangeFinished = { parameters = parameters.copy(noiseFactor = it) },
                    onValueChange = { parameters = parameters.copy(noiseFactor = it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        key(paletteVersion) {
            ExampleRow(maxItemsInEachRow = 2) {
                Example("背景底色 · 含透明度", Modifier.weight(1f)) {
                    ColorPicker(
                        initColor = parameters.backgroundColor,
                        onColorChanged = { parameters = parameters.copy(backgroundColor = it) },
                        onColorChangeFinished = { parameters = parameters.copy(backgroundColor = it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Example("Tint · 含透明度", Modifier.weight(1f)) {
                    ColorPicker(
                        initColor = parameters.tintColor,
                        onColorChanged = { parameters = parameters.copy(tintColor = it) },
                        onColorChangeFinished = { parameters = parameters.copy(tintColor = it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Box(
            Modifier.fillMaxWidth()
                .height(Theme.size.cell3)
                .clip(Theme.shape.v3)
                .background(Theme.color.backgroundVariant),
        ) {
            SourceScene(state, modifier = Modifier.matchParentSize())
            TargetLabel(
                state = state,
                title = "前景文字保持清晰",
                modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.65f).align(Alignment.Center),
                parameters = parameters,
            )
        }
        GalleryParagraph("修改 BlurParameters 并通过 copy 调整参数。边缘保留原始背景以便比较；半径可设为零，质量与噪点可拖到 0 或 1，两个颜色选择器均支持透明度。")
    }

    @Composable
    private fun MultipleTargetsExample() {
        val state = remember { BlurState() }
        var position by rememberValueState(0.5f)

        Slider(
            value = position,
            onValueChangeFinished = { position = it },
            onValueChange = { position = it },
            modifier = Modifier.fillMaxWidth()
        )
        GalleryCaption("拖动滑块移动右侧面板，调整窗口大小以检查来源与 Target 的对齐。")

        Box(
            Modifier.fillMaxWidth()
                .height(Theme.size.cell2)
                .clip(Theme.shape.v3)
                .background(Theme.color.backgroundVariant),
        ) {
            SourceScene(state, modifier = Modifier.matchParentSize())
            TargetLabel(
                state = state,
                title = "轻模糊",
                parameters = BlurParameters(
                    blurRadius = 4.dp,
                    backgroundColor = Color.Transparent,
                    tintColor = Color.White.copy(alpha = 0.12f),
                    noiseFactor = 0f,
                    qualityFraction = 1f,
                ),
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight(0.45f)
                    .align(Alignment.CenterStart)
                    .padding(Theme.padding.value),
            )
            TargetLabel(
                state = state,
                title = "重模糊 · 圆角",
                parameters = BlurParameters(
                    blurRadius = 28.dp,
                    backgroundColor = Theme.color.surface.copy(alpha = 0.75f),
                    tintColor = Theme.color.primary.copy(alpha = 0.2f),
                    noiseFactor = 0.3f,
                    qualityFraction = 0f,
                ),
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .fillMaxHeight(0.45f)
                    .align(BiasAlignment(0.9f, position * 1.8f - 0.9f))
                    .padding(Theme.padding.value),
            )
        }
    }

    @Composable
    private fun LifecycleExample() {
        var state by rememberRefState { BlurState() }
        var animate by rememberTrue()
        var showSource by rememberTrue()
        var showTarget by rememberTrue()

        GalleryActions {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                Switch(animate, { animate = it })
                Text("动态背景")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                Switch(showSource, { showSource = it })
                Text("显示 Source")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.padding.g)) {
                Switch(showTarget, { showTarget = it })
                Text("显示 Target")
            }
            PrimaryTextButton("替换 State", onClick = { state = BlurState() })
        }

        Box(
            Modifier.fillMaxWidth().height(Theme.size.cell3)
                .clip(Theme.shape.v3).background(Theme.color.backgroundVariant),
        ) {
            if (showSource) SourceScene(state, animated = animate, modifier = Modifier.matchParentSize())
            if (showTarget) {
                TargetLabel(
                    state = state,
                    title = "动态采样 · 清晰前景",
                    modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.65f).align(Alignment.Center),
                )
            }
        }
        GalleryParagraph("关闭 Source 可检查最后一帧保留，再开启检查恢复；关闭 Target 后来源继续正常绘制。也可以切换页面、替换 State、调整窗口及切换前后台，检查资源与画面恢复。")
    }

    @Composable
    private fun SourceScene(state: BlurState, modifier: Modifier, animated: Boolean = false) {
        var phase by rememberValueState(0f)
        LaunchedEffect(animated) {
            if (animated) {
                val start = withFrameNanos { it }
                while (isActive) {
                    withFrameNanos { time ->
                        phase = ((time - start) % 6_000_000_000L) / 6_000_000_000f
                    }
                }
            }
        }

        val primary = Theme.color.primaryContainer
        val secondary = Theme.color.secondaryContainer
        val tertiary = Theme.color.tertiaryContainer
        Box(modifier.blurSource(state)) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.linearGradient([
                    primary.copy(alpha = 0.7f),
                    secondary.copy(alpha = 0.7f),
                    tertiary.copy(alpha = 0.7f),
                ]))
                val stripe = size.width / 12f
                for (index in 0 ..< 16) {
                    val left = ((index + phase * 4f) % 16f - 2f) * stripe
                    drawRect(
                        if (index % 2 == 0) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.55f),
                        topLeft = Offset(left, 0f),
                        size = Size(stripe / 2f, size.height),
                    )
                }
                drawCircle(
                    color = tertiary,
                    radius = size.minDimension / 3f,
                    center = Offset(size.width * (0.25f + phase * 0.5f), size.height * 0.55f),
                )
            }
            Text(
                "SOURCE  ·  原始背景",
                color = Theme.color.onContainer,
                modifier = Modifier.align(Alignment.BottomStart).padding(Theme.padding.value5),
            )
        }
    }

    @Composable
    private fun TargetLabel(
        state: BlurState,
        title: String,
        modifier: Modifier,
        parameters: BlurParameters = BlurParameters(
            blurRadius = 10.dp,
            backgroundColor = Color(0xDD292929),
            tintColor = Color(0x6C292929),
            noiseFactor = 0.15f,
            qualityFraction = 0.66667f,
        ),
    ) {
        Box(
            modifier = modifier
                .clip(Theme.shape.v3)
                .border(Theme.border.v7, Theme.color.outline, Theme.shape.v3)
                .blurTarget(state, parameters)
                .padding(Theme.padding.value5),
            contentAlignment = Alignment.Center,
        ) {
            Text(title, color = Color.White, style = Theme.typography.v6)
        }
    }
}
