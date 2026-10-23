package love.yinlin.compose.ui.image

import android.graphics.drawable.AnimatedImageDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.image.data.ImageQualityPolicy
import love.yinlin.compose.image.data.ImageResult
import love.yinlin.compose.image.decoder.AndroidAnimationFactory
import love.yinlin.compose.image.decoder.WebpImageFormat
import love.yinlin.compose.window.rememberOffScreenWindowState
import love.yinlin.extension.catchingNull

@Composable
internal actual fun rememberPlatformAnimatedPainter(result: ImageResult, state: ImageState): Painter? {
    val animation = result.animation ?: return null
    if (!WebpImageFormat.matches(animation.bytes)) return null
    val platform = animation.platform ?: return null

    // 接管已经解码好的实例不执行 I/O；首次组合可直接绘制系统播放器。
    var drawable by rememberState<AnimatedImageDrawable?>(result) { platform.acquire() }
    var unsupported by rememberState(result) { false }

    LaunchedEffect(result) {
        if (drawable != null) return@LaunchedEffect
        val decoded = catchingNull { AndroidAnimationFactory.create(animation) }
        if (decoded == null) unsupported = true
        else drawable = decoded
    }

    if (unsupported) return null

    val painter = remember(drawable) { drawable?.let(::AndroidAnimatedPainter) }
    if (painter != null) {
        val foreground by rememberOffScreenWindowState()
        val playing = state.isPlaying && state.visible && foreground
        SideEffect { painter.update(playing) }
        DisposableEffect(painter) { onDispose(painter::dispose) }
        return painter
    }

    // 等待独立系统播放器时显示 loader 已经取得的首帧，不启动第二条 Kotlin 逐帧播放任务
    return remember(result) {
        result.image.prepareToDraw()
        BitmapPainter(result.image, filterQuality = ImageQualityPolicy.from(animation.request.quality).filterQuality)
    }
}