package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.extension.Reference

@Stable
expect abstract class PlatformApplication<out A : PlatformApplication<A>>(
    self: Reference<A?>,
    delegate: PlatformContextDelegate,
) : Application<A>