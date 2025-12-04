package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.extension.BaseLazyReference

@Stable
expect abstract class PlatformApplication<out A : PlatformApplication<A>>(
    self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>