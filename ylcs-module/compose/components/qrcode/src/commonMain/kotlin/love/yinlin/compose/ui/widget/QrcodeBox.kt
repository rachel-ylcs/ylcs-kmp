package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.image.Image
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun QrcodeBox(
    text: String,
    modifier: Modifier = Modifier,
    logo: DrawableResource? = null,
) {
    val logoPainter = logo?.let { painterResource(it) }
    val primaryColor = Theme.color.primary
    val secondaryColor = Theme.color.secondary

    val painter = rememberQrCodePainter(data = text) {
        logo {
            painter = logoPainter
            padding = QrLogoPadding.Natural(1f)
            shape = QrLogoShape.circle()
            size = 0.25f
        }
        shapes {
            ball = QrBallShape.circle()
            darkPixel = QrPixelShape.roundCorners()
            frame = QrFrameShape.roundCorners(0.25f)
        }
        colors {
            dark = QrBrush.brush {
                Brush.linearGradient(
                    0f to primaryColor.copy(alpha = 0.9f),
                    1f to secondaryColor.copy(0.9f),
                    end = Offset(it, it)
                )
            }
            frame = QrBrush.solid(primaryColor)
        }
    }

    Image(painter = painter, modifier = modifier)
}