package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException

/**
 * 为内容提供由指定图像生成的调色板。
 *
 * [bitmap] 是唯一采样输入，内容槽只负责布局与展示，不会参与取色。请将传入图像视为不可变对象；
 * 图像内容更新时传入新的 [ImageBitmap] 实例。图像变化会取消旧分析并启动新分析。
 *
 * @param bitmap 待采样图像
 * @param modifier 容器修饰符
 * @param content 使用调色板结果的内容
 */
@Composable
fun PaletteBox(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    content: @Composable PaletteScope.() -> Unit,
) {
    val state = remember { PaletteState() }
    val paletteScope = remember(state) { PaletteScope(state) }

    LaunchedEffect(bitmap) {
        state.beginGeneration()
        try {
            state.publish(Palette.from(bitmap).generateAsync())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            state.fail(error)
        }
    }

    Box(modifier) {
        paletteScope.content()
    }
}
