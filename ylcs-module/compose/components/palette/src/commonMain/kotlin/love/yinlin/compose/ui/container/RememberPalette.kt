package love.yinlin.compose.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.extension.catchingError
import love.yinlin.extension.then

/**
 * 从 [bitmap] 异步生成并记忆调色板
 *
 * @param bitmap 待采样图像
 */
@Composable
fun rememberPalette(bitmap: ImageBitmap, builder: Palette.Builder.() -> Unit = { }): PaletteState {
    val palette = remember { PaletteState() }

    LaunchedEffect(bitmap) {
        palette.beginGeneration()

        catchingError {
            palette.publish(Palette.from(bitmap, builder).generate())
        }?.then(palette::fail)
    }

    return palette
}
