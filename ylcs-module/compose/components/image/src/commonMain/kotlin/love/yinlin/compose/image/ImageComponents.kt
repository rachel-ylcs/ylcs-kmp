package love.yinlin.compose.image

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions
import love.yinlin.compose.image.decoder.*
import love.yinlin.compose.image.input.*
import kotlin.reflect.KClass

@Stable
internal class ImageComponents(customInputs: List<ImageInput<*>>, customFormats: List<ImageFormat>) {
    val platformFilesAllowed: Boolean = customFormats.isEmpty() && supportsPlatformImageFiles
    private val inputs: Map<KClass<out ImageSource>, ImageInput<*>>
    private val formats: List<ImageFormat>

    init {
        val builtInInputs: List<ImageInput<*>> = [
            WebImageInput, LocalFileImageInput, ResourceFileImageInput, ByteArrayImageInput,
        ]
        val builtInFormats: List<ImageFormat> = [
            JpegImageFormat, PngImageFormat, BmpImageFormat, GifImageFormat, WebpImageFormat,
        ]
        inputs = (builtInInputs + customInputs).associateBy { it.sourceClass }
        formats = (builtInFormats + customFormats).asReversed()
    }

    @Suppress("UNCHECKED_CAST")
    fun input(source: ImageSource): ImageInput<ImageSource> = checkNotNull(inputs[source::class]) { "Image input is not configured for this source type" } as ImageInput<ImageSource>

    fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder {
        val format = formats.firstOrNull { it.matches(bytes) } ?: error("Image format is not configured")
        // 无关格式注册不影响内置 WebP；自定义逐帧实现仍保留完整控制权
        options.platformAnimationAllowed = format === WebpImageFormat
        return format.open(bytes, options)
    }
}
