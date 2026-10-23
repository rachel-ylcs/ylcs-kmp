package love.yinlin.compose.image.decoder

import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.image.data.ImageDecodeOptions
import org.jetbrains.skia.SamplingMode

internal val ImageDecodeOptions.samplingMode: SamplingMode get() = when (quality) {
    ImageQuality.Low, ImageQuality.Medium -> SamplingMode.LINEAR
    ImageQuality.High -> SamplingMode.MITCHELL
    ImageQuality.Full -> SamplingMode.CATMULL_ROM
}