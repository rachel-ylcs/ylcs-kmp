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
     * Desktop -> 最小化
     * Android -> 返回桌面
     */
    fun backHome()

    /**
     * 打开Uri
     */
    fun openUri(uri: Uri): Boolean

    /**
     * 复制文本
     */
    fun copyText(text: String): Boolean

    /**
     * 隐式Uri
     */
    fun implicitFileUri(uri: Uri): ImplicitUri
}