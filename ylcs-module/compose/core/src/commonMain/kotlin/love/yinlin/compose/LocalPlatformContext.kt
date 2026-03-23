package love.yinlin.compose

import love.yinlin.compose.extension.localComposition
import love.yinlin.foundation.PlatformContextProvider

val LocalPlatformContext = localComposition<PlatformContextProvider>()