package love.yinlin.compose

import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.uri.Uri

@Stable
expect abstract class PlatformApplication<out A : PlatformApplication<A>>(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A> {
    fun openUri(uri: Uri): Boolean
    fun copyText(text: String): Boolean
}