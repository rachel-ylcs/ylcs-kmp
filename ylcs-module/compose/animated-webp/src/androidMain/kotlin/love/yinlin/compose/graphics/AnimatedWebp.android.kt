package love.yinlin.compose.graphics

import love.yinlin.extension.catchingNull

private class AndroidAnimatedWebp(
    width: Int,
    height: Int,
    frameCount: Int
) : AnimatedWebp(width, height, frameCount) {
    override fun close() { }
}

actual fun ByteArray.decodeAnimatedWebp(): AnimatedWebp? = catchingNull {
    null
}