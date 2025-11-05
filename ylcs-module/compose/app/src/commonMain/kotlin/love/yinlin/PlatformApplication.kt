package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.extension.LazyReference

@Stable
expect abstract class PlatformApplication<out A : PlatformApplication<A>>(
    self: LazyReference<A>,
    delegate: PlatformContextDelegate,
) : Application<A>