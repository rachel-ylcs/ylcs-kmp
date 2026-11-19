package love.yinlin.compose.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从 [bitmap] 异步生成并记忆调色板。
 *
 * 同一图像实例在当前组合位置只生成一次；传入新的 [ImageBitmap] 实例时，会创建新的状态并重新分析。
 * 图像分析在 [Dispatchers.Default] 执行，不会阻塞 UI 线程。请将图像视为不可变对象。
 *
 * @param bitmap 待采样图像
 * @return 可直接读取颜色、Swatch、生成状态和错误的调色板状态
 */
@Composable
fun rememberPalette(bitmap: ImageBitmap): PaletteState {
    val palette = remember(bitmap) { PaletteState() }

    LaunchedEffect(bitmap) {
        palette.beginGeneration()
        try {
            val result = withContext(Dispatchers.Default) {
                Palette.from(bitmap).generate()
            }
            palette.publish(result)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            palette.fail(error)
        }
    }

    return palette
}
