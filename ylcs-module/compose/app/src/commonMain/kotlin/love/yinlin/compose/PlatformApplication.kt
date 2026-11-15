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
     * 请求系统打开 URI
     */
    suspend fun openUri(uri: String): Boolean

    /**
     * 复制文本
     */
    suspend fun copyText(text: String): Boolean

    /**
     * 隐式Uri
     */
    fun implicitFileUri(uri: Uri): ImplicitUri
}
