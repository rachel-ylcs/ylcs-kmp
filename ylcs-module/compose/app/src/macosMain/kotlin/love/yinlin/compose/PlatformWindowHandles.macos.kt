@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package love.yinlin.compose

internal actual var PlatformApplication<*>.windowHandle: Long?
    get() = windowHandle
    set(value) { windowHandle = value }

internal actual var PlatformApplication<*>.displayHandle: Long?
    get() = null
    set(value) = Unit
