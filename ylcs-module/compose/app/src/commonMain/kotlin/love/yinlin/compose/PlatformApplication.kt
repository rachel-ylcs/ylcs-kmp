package love.yinlin.compose

import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContext
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.Uri

@Stable
expect abstract class PlatformApplication<out A : PlatformApplication<A>>(
    self: BaseLazyReference<A>,
    context: PlatformContext,
) : Application<A> {
    /**
     * 返回桌面
     *
     * JVM / Native Desktop -> 最小化
     * Android -> 返回桌面
     * iOS、Web -> 暂不执行操作
     */
    fun backHome()

    /**
     * 打开Uri。Native Desktop 自动切换到窗口主线程，其他平台保留调用上下文。
     */
    suspend fun openUri(uri: Uri): Boolean

    /**
     * 复制文本。Native Desktop 自动切换到窗口主线程，其他平台保留调用上下文。
     */
    suspend fun copyText(text: String): Boolean

    /**
     * 隐式Uri
     */
    fun implicitFileUri(uri: Uri): ImplicitUri
}
