package love.yinlin.startup

import android.graphics.Bitmap
import com.github.panpf.sketch.BitmapImage
import com.github.panpf.sketch.Image
import com.github.panpf.sketch.annotation.WorkerThread
import com.github.panpf.sketch.asImage
import com.github.panpf.sketch.decode.DecodeConfig
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.internal.decode
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.DataSource
import okio.BufferedSink

@WorkerThread
actual fun compressFixed(image: Image, sink: BufferedSink) {
    require(image is BitmapImage) { "Unsupported image type: ${image::class}" }
    image.bitmap.compress(Bitmap.CompressFormat.PNG, 100, sink.outputStream())
}

@WorkerThread
actual fun decodeFixed(requestContext: RequestContext, imageInfo: ImageInfo, dataSource: DataSource): Image {
    val decodeConfig = DecodeConfig(
        request = requestContext.request,
        mimeType = imageInfo.mimeType,
        isOpaque = false
    )
    val bitmap = dataSource.decode(decodeConfig)
    return bitmap.asImage()
}

actual fun supportImageFixed(image: Image): Boolean = image is BitmapImage