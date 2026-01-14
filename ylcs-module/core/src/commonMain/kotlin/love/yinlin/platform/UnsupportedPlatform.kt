package love.yinlin.platform

val UnsupportedPlatformText = "不支持的平台 $platform"

open class UnsupportedPlatformException : Exception(UnsupportedPlatformText)

fun unsupportedPlatform(): Nothing = throw UnsupportedPlatformException()