package love.yinlin.compose.image.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder as AndroidDecoder
import android.graphics.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntSize
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.GifInfo
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.data.WebpInfo
import java.nio.ByteBuffer
import androidx.core.graphics.scale
import love.yinlin.extension.catchingDefault
import love.yinlin.io.ByteArrayIO
import love.yinlin.io.Endian

internal actual fun openPlatformImageDecoder(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    val maxDecodePixels = ImageManager.configuration.maxDecodePixels
    // 平台静态解码只取首帧，不建立动画容器/帧表。
    if (!request.animated && Build.VERSION.SDK_INT >= 28) return decodeModernBitmap(bytes, request)
    if (!request.animated && !WebpImageFormat.matches(bytes)) return decodeLegacyBitmap(bytes, request)

    val gif = bytes.size >= 6 && bytes.decodeToString(0, 3) == "GIF"
    val webp = bytes.size >= 30 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 16) == "WEBPVP8X" && bytes[20].toInt() and 2 != 0

    return when {
        gif -> {
            val info = GifInfo(bytes, firstFrameOnly = !request.animated)
            require(info.size.width.toLong() * info.size.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }
            AndroidGifDecoder(bytes, info, request)
        }
        webp -> {
            val info = WebpInfo(bytes, firstFrameOnly = !request.animated)
            require(info.size.width.toLong() * info.size.height <= maxDecodePixels) { "Animation exceeds the frame pixel budget" }
            AndroidWebpDecoder(bytes, info, request)
        }
        Build.VERSION.SDK_INT >= 28 -> decodeModernBitmap(bytes, request)
        else -> decodeLegacyBitmap(bytes, request)
    }
}

@RequiresApi(28)
private fun decodeModernBitmap(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    var original = IntSize.Zero
    val source = AndroidDecoder.createSource(ByteBuffer.wrap(bytes))
    val bitmap = AndroidDecoder.decodeBitmap(source) { decoder, info, _ ->
        original = IntSize(info.size.width, info.size.height)
        val size = request.sampledSize(original)
        decoder.allocator = AndroidDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        decoder.setTargetSize(size.width, size.height)
    }
    return AndroidBitmapDecoder(bitmap, original)
}

private fun readTiffOrientation(input: ByteArrayIO, start: Int, end: Int): Int {
    val endian = when (input.readString(start, 2)) {
        "II" -> Endian.LITTLE
        "MM" -> Endian.BIG
        else -> return 1
    }
    require(input.readUShort(start + 2, endian).toInt() == 42)

    val relative = input.readUInt(start + 4, endian).toLong()
    require(relative >= 8 && relative <= end.toLong() - start - 2)

    val directory = start + relative.toInt()
    val count = input.readUShort(directory, endian).toInt()
    require(count.toLong() * 12 <= end.toLong() - directory - 2)

    for (index in 0 ..< count) {
        val entry = directory + 2 + index * 12
        if (input.readUShort(entry, endian).toInt() == 0x0112 && input.readUShort(entry + 2, endian).toInt() == 3 &&
            input.readUInt(entry + 4, endian) == 1u) {
            return input.readUShort(entry + 8, endian).toInt().takeIf { it in 1 .. 8 } ?: 1
        }
    }
    return 1
}

private fun jpegExifOrientation(bytes: ByteArray): Int = catchingDefault(1) {
    val input = ByteArrayIO(bytes)
    if (input.readUShort(0).toInt() != 0xffd8) return@catchingDefault 1

    var offset = 2
    while (offset < bytes.size) {
        require(input.readUByte(offset++).toInt() == 0xff)

        var marker = input.readUByte(offset++).toInt()

        while (marker == 0xff) marker = input.readUByte(offset++).toInt()

        if (marker == 0xda || marker == 0xd9) break
        if (marker == 0x01 || marker in 0xd0 .. 0xd7) continue
        val length = input.readUShort(offset).toInt()
        require(length >= 2 && length <= bytes.size - offset)

        if (marker == 0xe1 && length >= 16 && input.readString(offset + 2, 6) == "Exif\u0000\u0000") {
            return@catchingDefault readTiffOrientation(input, offset + 8, offset + length)
        }

        offset += length
    }
    1
}

private fun decodeLegacyBitmap(bytes: ByteArray, request: ImageDecodeOptions): ImageDecoder {
    val bounds = BitmapFactory.Options()
    bounds.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val orientation = jpegExifOrientation(bytes)
    val rotate = orientation in 5 .. 8
    val original = if (rotate) IntSize(bounds.outHeight, bounds.outWidth) else IntSize(bounds.outWidth, bounds.outHeight)
    val size = request.sampledSize(original)
    val target = if (rotate) IntSize(size.height, size.width) else size
    var sample = 1

    while (bounds.outWidth / (sample * 2) >= target.width && bounds.outHeight / (sample * 2) >= target.height) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)) { "Unsupported image data" }

    if (orientation != 1) {
        val matrix = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                6 -> setRotate(90f)
                7 -> {
                    setRotate(270f)
                    postScale(-1f, 1f)
                }
                8 -> setRotate(270f)
            }
        }
        val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (transformed !== bitmap) bitmap.recycle()
        bitmap = transformed
    }

    val scaled = bitmap.scale(size.width, size.height)
    if (scaled !== bitmap) bitmap.recycle()
    return AndroidBitmapDecoder(scaled, original)
}
