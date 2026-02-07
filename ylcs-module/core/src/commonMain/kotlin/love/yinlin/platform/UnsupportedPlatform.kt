package love.yinlin.platform

val UnsupportedPlatformText = "unsupported platform [$platform]"

open class UnsupportedPlatformException : Exception(UnsupportedPlatformText)

fun unsupportedPlatform(): Nothing = throw UnsupportedPlatformException()