package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.graphics.qrcode.QrcodeECLevel
import love.yinlin.compose.graphics.qrcode.QrcodeShape
import love.yinlin.compose.graphics.qrcode.rememberQrcodePainter
import love.yinlin.compose.ui.image.Image
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun QrcodeBox(
    text: String,
    modifier: Modifier = Modifier,
    logo: DrawableResource? = null,
    errorCorrectionLevel: QrcodeECLevel = QrcodeECLevel.H,
    shape: QrcodeShape = QrcodeShape.Square,
    color: Color = Theme.color.primary,
    secondaryColor: Color = Theme.color.secondary,
    background: Color = Colors.Transparent,
) {
    val painter = rememberQrcodePainter(
        text = text,
        logo = logo?.let { painterResource(it) },
        shape = shape,
        color = color,
        secondaryColor = secondaryColor,
        ecLevel = errorCorrectionLevel,
        background = background,
    )
    Image(painter = painter, modifier = modifier)
}
