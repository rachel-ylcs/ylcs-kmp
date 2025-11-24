package love.yinlin.compose.game

import androidx.compose.ui.graphics.ImageBitmap

sealed interface Asset {
    data class Image(val image: ImageBitmap) : Asset
    data class Animation(val image: ImageBitmap, val count: Int): Asset
}