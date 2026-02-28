package love.yinlin.compose.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.*
import love.yinlin.compose.data.CropRegion
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.CropImage
import love.yinlin.compose.ui.image.CropState
import love.yinlin.compose.ui.input.TextButton

@Stable
class DialogCrop : Dialog<CropRegion>() {
    private var url: String? by mutableStateOf(null)
    private var aspectRatio: Float by mutableFloatStateOf(0f)
    private val cropState = CropState()

    suspend fun open(url: String, aspectRatio: Float = 0f): CropRegion? {
        this.url = url
        this.aspectRatio = aspectRatio
        this.cropState.reset()
        return awaitResult()
    }

    @Composable
    override fun Land() {
        LandFloating {
            Column(modifier = Modifier.fillMaxSize().background(Colors.Black)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        text = "取消",
                        icon = Icons.ArrowBack,
                        color = Theme.color.secondary,
                        onClick = ::close
                    )
                    TextButton(
                        text = "裁剪",
                        icon = Icons.CropSquare,
                        color = Theme.color.primary,
                        onClick = { future?.send(cropState.result) }
                    )
                }
                CropImage(
                    url = url,
                    aspectRatio = aspectRatio,
                    state = cropState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}