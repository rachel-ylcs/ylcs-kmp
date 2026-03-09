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
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.SecondaryTextButton

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