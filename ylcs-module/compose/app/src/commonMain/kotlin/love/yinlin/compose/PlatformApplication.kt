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
    fun backHome()
    fun openUri(uri: Uri): Boolean
    fun copyText(text: String): Boolean
    fun implicitFileUri(uri: Uri): ImplicitUri
}