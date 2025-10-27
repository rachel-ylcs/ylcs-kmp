package love.yinlin.compose.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.*
import love.yinlin.compose.data.ImageCropResult
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.image.CropImage
import love.yinlin.compose.ui.image.CropState

@Stable
class FloatingDialogCrop : FloatingDialog<ImageCropResult>() {
    private var url: String? by mutableStateOf(null)
    private var aspectRatio: Float by mutableFloatStateOf(0f)
    private val cropState = CropState()

    suspend fun openSuspend(url: String, aspectRatio: Float = 0f): ImageCropResult? {
        this.url = url
        this.aspectRatio = aspectRatio
        this.cropState.reset()
        return awaitResult()
    }

    @Composable
    override fun Wrapper(block: @Composable (() -> Unit)) {
        super.Wrapper {
            Column(
                modifier = Modifier.width(CustomTheme.size.dialogWidth).background(Colors.Black),
                horizontalAlignment = Alignment.End
            ) {
                CropImage(
                    url = url,
                    aspectRatio = aspectRatio,
                    state = cropState,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
                ClickText(
                    text = "裁剪",
                    color = Colors.White,
                    modifier = Modifier.padding(CustomTheme.padding.equalValue),
                    onClick = {
                        future?.send(cropState.result)
                    }
                )
            }
        }
    }
}