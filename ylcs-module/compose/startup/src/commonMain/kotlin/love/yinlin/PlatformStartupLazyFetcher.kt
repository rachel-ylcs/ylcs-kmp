package love.yinlin

import love.yinlin.platform.Platform

inline fun <T : Any> usePlatformStartupLazyFetcher(
    vararg platform: Platform,
    crossinline factory: () -> T,
): StartupLazyFetcher<T?> = StartupLazyFetcher {
    Platform.use(
        *platform,
        ifTrue = factory,
        ifFalse = { null }
    )
}

inline fun <T : Any> useNotPlatformStartupLazyFetcher(
    vararg platform: Platform,
    crossinline factory: () -> T,
): StartupLazyFetcher<T?> = StartupLazyFetcher {
    Platform.use(
        *platform,
        ifTrue = { null },
        ifFalse = factory
    )
}