package love.yinlin.compose.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.Text
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
private fun HelpLayout1() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
    ) {
        Row(
            modifier = Modifier.widthIn(max = Theme.size.cell1).fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val yFractions = remember { List(32) { Random.nextFloat() } }

            SimpleClipText(text = "辨律", style = Theme.typography.v4.bold)
            Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().drawWithContent {
                val w = size.width
                val h = size.height
                val centerY = h / 2
                val startX = w * 0.1f
                val endX = w * 0.9f
                val waveWidth = endX - startX
                val stepX = waveWidth / (yFractions.size - 1)
                val path = Path().apply {
                    moveTo(0f, centerY)
                    lineTo(startX, centerY)
                    yFractions.fastForEachIndexed { i, f -> lineTo(startX + i * stepX, f * h) }
                    lineTo(endX, centerY)
                    lineTo(w, centerY)
                }
                drawPath(path = path, color = Colors.Cyan4, style = Stroke(width = 3f, cap = StrokeCap.Round))
            })
        }
        Text(
            text = "在安静的环境下认真聆听歌曲的韵律，捕捉乐声的起伏，追踪交互的音效，掌握音符间的节奏。",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpLayout2() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
    ) {
        Row(
            modifier = Modifier.widthIn(max = Theme.size.cell1).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleClipText(text = "观微", style = Theme.typography.v4.bold)
            Box(modifier = Modifier.weight(1f).aspectRatio(1f).drawWithContent {
                drawRect(Colors.Purple4, style = Stroke(3f))
            })
            Box(modifier = Modifier.weight(1f).aspectRatio(1f).drawWithContent {
                drawCircle(Colors.Orange4, style = Stroke(3f))
            })
            Box(modifier = Modifier.weight(1f).aspectRatio(1f).drawWithContent {
                val rect = Rect(Offset.Zero, size)
                drawLine(Colors.Yellow4, rect.topCenter, rect.centerLeft, strokeWidth = 3f)
                drawLine(Colors.Yellow4, rect.topCenter, rect.centerRight, strokeWidth = 3f)
                drawLine(Colors.Yellow4, rect.bottomCenter, rect.centerLeft, strokeWidth = 3f)
                drawLine(Colors.Yellow4, rect.bottomCenter, rect.centerRight, strokeWidth = 3f)
            })
        }
        Text(
            text = """
观察地图中出现的音符颜色与音级来判断它的类型，并在其限制的时间内采取合适的按键方式完成交互。
1. 单音(方形)：点按对应音符的按键。
2. 延音(圆形)：保持长按对应音符的按键。
3. 连音(菱形)：按顺序依次点按音符按键。
                    """.trimIndent(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpLayout3() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
    ) {
        Row(
            modifier = Modifier.widthIn(max = Theme.size.cell1).fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleClipText(text = "拨弦", style = Theme.typography.v4.bold)
            Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().drawWithContent {
                val w = size.width
                val h = size.height
                val sideWidth = w * 0.1f
                val length = w * 0.7f
                val tiltOffset = h * 0.2f
                val thickness = h * 0.8f
                val woodColor = Color(0xFF8B4513)
                val stringColor = Color(0xFFE0E0E0)
                val leftPath = Path().apply {
                    moveTo(0f, tiltOffset)
                    lineTo(sideWidth, 0f)
                    lineTo(sideWidth, thickness)
                    lineTo(0f, tiltOffset + thickness)
                    close()
                }
                val rightPath = Path().apply {
                    moveTo(length, tiltOffset)
                    lineTo(length + sideWidth, 0f)
                    lineTo(length + sideWidth, thickness)
                    lineTo(length, tiltOffset + thickness)
                    close()
                }
                drawPath(leftPath, color = woodColor)
                drawPath(rightPath, color = woodColor)
                val stringCount = 5
                for (i in 0 until stringCount) {
                    val relativePos = i.toFloat() / (stringCount - 1)
                    val offsetX = relativePos * sideWidth
                    val offsetY = (1f - relativePos) * tiltOffset
                    drawLine(
                        color = stringColor,
                        start = Offset(offsetX, offsetY),
                        end = Offset(length + offsetX, offsetY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            })
        }
        Text(
            text = "手指动作轻盈飞快，根据简谱中不同的音符类型在相应位置完成交互，合理运用琴韵避免出错或遗漏，保持连击不断，争取获得高分。",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HelpLayout4() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
    ) {
        Row(
            modifier = Modifier.widthIn(max = Theme.size.cell1).fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleClipText(text = "如一", style = Theme.typography.v4.bold)
            Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().drawWithContent {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val angles = listOf(-45f, 0f, 45f)

                translate(left = centerX, top = h) {
                    angles.forEach { angle ->
                        val rad = angle / 180 * 3.14159f
                        val absRad = abs(rad)
                        val lenToTop = if (cos(absRad.toDouble()) > 0) h / cos(absRad.toDouble()).toFloat() else Float.MAX_VALUE
                        val lenToSide = if (sin(absRad.toDouble()) > 0) (w / 2f) / sin(absRad.toDouble()).toFloat() else Float.MAX_VALUE
                        val currentLeafLength = minOf(lenToTop, lenToSide)
                        val currentLeafWidth = w * 0.2f
                        rotate(degrees = angle, pivot = Offset(0f, 0f)) {
                            val leafPath = Path().apply {
                                moveTo(0f, 0f)
                                quadraticTo(-currentLeafWidth, -currentLeafLength * 0.5f, 0f, -currentLeafLength)
                                quadraticTo(currentLeafWidth, -currentLeafLength * 0.5f, 0f, 0f)
                            }
                            drawPath(path = leafPath, color = Color(0xFF4CAF50).copy(alpha = 0.7f))
                            drawPath(path = leafPath, color = Color(0xFF2E7D32), style = Stroke(width = 3f))
                        }
                    }
                }
            })
        }
        Text(
            text = "慎终如始，则无败事。坚持游玩歌曲到结束以获得胜利，反复挑战突破自我。",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun GameHelpLayout() {
    val device by rememberDeviceType()

    Box(
        modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current),
        contentAlignment = Alignment.Center
    ) {
        if (device == Device.Type.PORTRAIT) {
            Column(
                modifier = Modifier
                    .width(Theme.size.cell1)
                    .fillMaxHeight()
                    .padding(vertical = Theme.padding.v5)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v1)
            ) {
                HelpLayout1()
                HelpLayout2()
                HelpLayout3()
                HelpLayout4()
            }
        }
        else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value5),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h5)
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v1)
                ) {
                    HelpLayout1()
                    HelpLayout2()
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v1)
                ) {
                    HelpLayout3()
                    HelpLayout4()
                }
            }
        }
    }
}