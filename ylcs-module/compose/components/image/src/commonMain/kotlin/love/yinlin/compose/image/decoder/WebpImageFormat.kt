package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object WebpImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.matchesImageSignature(0, "RIFF") && bytes.matchesImageSignature(8, "WEBP")

    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder {
        if (!options.animated) return openPlatformImageDecoder(bytes, options)

        // 简单格式没有动画；扩展格式的 VP8X 动画位为 0x02。
        // 未识别或不完整的头部仍交给平台解码器校验。
        val static = bytes.size >= 20 && (bytes.matchesImageSignature(12, "VP8 ") || bytes.matchesImageSignature(12, "VP8L") ||
            (bytes.size >= 30 && bytes.matchesImageSignature(12, "VP8X") && bytes[16] == 10.toByte() &&
                bytes[17] == 0.toByte() && bytes[18] == 0.toByte() && bytes[19] == 0.toByte() && (bytes[20].toInt() and 2) == 0))

        return openPlatformImageDecoder(bytes, if (static) options.staticOptions() else options)
    }
}
