package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 先绘制背景层，再在其上绘制内容，并为内容提供基于背景图像生成的调色板。
 *
 * 背景通过可复用 GraphicsLayer 记录；颜色分析只读取缩小后的采样层，并在后台协程中执行。
 * 背景绘制失效会触发重新采样；连续变化时最多每 400ms 分析一次，且相同像素内容会跳过量化。
 *
 * @param modifier 容器修饰符
 * @param backdrop 背景内容，决定容器尺寸
 * @param content 覆盖在背景上的内容及调色板作用域
 */
@Composable
fun PaletteBox(
    modifier: Modifier = Modifier,
    backdrop: @Composable BoxScope.() -> Unit,
    content: @Composable PaletteScope.() -> Unit,
) {
    val state = remember { PaletteState() }
    val coroutineScope = rememberCoroutineScope()
    val controller = remember(coroutineScope, state) { PaletteCaptureController(coroutineScope, state) }
    val paletteScope = remember(state, controller) { PaletteScope(state, controller) }
    val backdropLayer = rememberGraphicsLayer()
    val sampleLayer = rememberGraphicsLayer()

    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }

    Box(modifier) {
        Box(
            modifier = Modifier.drawWithContent {
                val backdropSize = size
                backdropLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(backdropLayer)

                if (backdropSize.width > 0f && backdropSize.height > 0f) {
                    val area = backdropSize.width * backdropSize.height
                    val ratio = min(1f, sqrt(Palette.DEFAULT_RESIZE_BITMAP_AREA.toDouble() / area.toDouble()).toFloat())
                    val sampleWidth = ceil(backdropSize.width * ratio).toInt().coerceAtLeast(1)
                    val sampleHeight = ceil(backdropSize.height * ratio).toInt().coerceAtLeast(1)

                    sampleLayer.record(size = IntSize(sampleWidth, sampleHeight)) {
                        scale(
                            scaleX = sampleWidth / backdropSize.width,
                            scaleY = sampleHeight / backdropSize.height,
                            pivot = Offset.Zero,
                        ) {
                            drawLayer(backdropLayer)
                        }
                    }
                    controller.onBackdropDraw(sampleLayer)
                }
            },
            content = backdrop,
        )

        Box(Modifier.matchParentSize()) {
            with(paletteScope) {
                content()
            }
        }
    }
}
