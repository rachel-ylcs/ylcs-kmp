package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import love.yinlin.annotation.CompatibleRachelApi

/**
 * CMP >= 1.13.0-beta01 版本后默认开启了 VectorPainter 优化
 *
 * JVM Desktop 在 Application 域下没有 LocalOwner 所以不能利用缓存保存 Vector 从而崩溃
 *
 * 使用下面的无缓存方法兼容新版
 */
@CompatibleRachelApi
@Composable
internal fun rememberWindowVectorPainter(image: ImageVector): Painter = rememberVectorPainter(
    defaultWidth = image.defaultWidth,
    defaultHeight = image.defaultHeight,
    viewportWidth = image.viewportWidth,
    viewportHeight = image.viewportHeight,
    name = image.name,
    tintColor = image.tintColor,
    tintBlendMode = image.tintBlendMode,
    autoMirror = image.autoMirror,
) { _, _ ->
    RenderVectorGroup(image.root)
}