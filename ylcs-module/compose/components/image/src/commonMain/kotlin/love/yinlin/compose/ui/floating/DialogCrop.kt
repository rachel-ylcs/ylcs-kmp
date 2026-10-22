package love.yinlin.compose.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.*
import love.yinlin.compose.data.CropRegion
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.BasicImage
import love.yinlin.compose.ui.image.CropImageState
import love.yinlin.compose.ui.image.ImageState
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.node.cropImageRegion

@Stable
class DialogCrop : Dialog<CropRegion>() {
    private var source: ImageSource? by mutableRefStateOf(null)
    private var aspectRatio: Float by mutableFloatStateOf(0f)

    private val imageState = ImageState()
    private val cropImageState = CropImageState()

    override val dismissOnClickOutside: Boolean = false

    suspend fun open(source: ImageSource, aspectRatio: Float = 0f): CropRegion? {
        this.source = source
        this.aspectRatio = aspectRatio
        this.cropImageState.reset()
        return awaitResult()
    }

    @Composable
    override fun Land() {
        LandFloating { _, _ ->
            Column(modifier = Modifier.fillMaxSize().background(Colors.Black).padding(LocalImmersivePadding.current)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryTextButton(
                        text = "取消",
                        icon = Icons.ArrowBack,
                        onClick = ::close
                    )
                    PrimaryTextButton(
                        text = "裁剪",
                        icon = Icons.CropSquare,
                        onClick = { future?.send(cropImageState.result) }
                    )
                }
                BasicImage(
                    source = source,
                    state = imageState,
                    modifier = Modifier.fillMaxWidth().weight(1f).cropImageRegion(
                        imageState = imageState,
                        cropImageState = cropImageState,
                        aspectRatio = aspectRatio
                    )
                )
            }
        }
    }
}
