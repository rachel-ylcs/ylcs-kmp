package love.yinlin.compose.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDarkMode
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.basicTextStyle
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.mainFont

@Stable
data class ModeShape(
    val small: Number,
    val medium: Number,
    val large: Number,
)

@Stable
data class ModeText(
    val isBold: Boolean,
    val small: Number,
    val medium: Number,
    val large: Number,
) {
    fun small(font: FontFamily): TextStyle = basicTextStyle(font, small.toDouble().sp, isBold)
    fun medium(font: FontFamily): TextStyle = basicTextStyle(font, medium.toDouble().sp, isBold)
    fun large(font: FontFamily): TextStyle = basicTextStyle(font, large.toDouble().sp, isBold)

    val style: TextStyle @Composable get() = mainFont().let { font ->
        when (LocalDevice.current.size) {
            Device.Size.SMALL -> small(font)
            Device.Size.MEDIUM -> medium(font)
            Device.Size.LARGE -> large(font)
        }
    }
}

val Number.shape: CornerBasedShape get() = RoundedCornerShape(this.toDouble().dp)

private fun Boolean.select(ifTrue: Color, ifFalse: Color): Color = if (this) ifTrue else ifFalse

@Stable
open class BaseCustomTypography {
    open val bodyExtraSmall: TextStyle @Composable get() = ModeText(false, 10, 11, 12).style
    open val displayExtraLarge: TextStyle @Composable get() = ModeText(false, 32, 34, 36).style
    open val rhymeDisplay: TextStyle @Composable get() = ModeText(false, 100, 100, 100).style
}

@Stable
open class BaseCustomColorScheme {
    open val warning: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Yellow4, Colors.Yellow5)
    open val onWarning: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Dark, Colors.White)
    open val backgroundVariant: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Dark, Colors.Ghost)
    open val onBackgroundVariant: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Ghost, Colors.Dark)
}

@Stable
open class BaseCustomSize {
    open val little: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    open val microIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(18, 20, 22)
    open val icon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(22, 24, 26)
    open val mediumIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(26, 28, 30)
    open val largeIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(30, 32, 34)
    open val extraIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(34, 36, 38)
    open val smallInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(36, 40, 44)
    open val mediumInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(44, 50, 56)
    open val largeInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(56, 64, 72)
    open val extraInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(72, 80, 88)
    open val sliderWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(90, 96, 102)
    open val fab: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(40, 44, 48)
    open val microImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(32, 40, 48)
    open val image: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(48, 56, 64)
    open val mediumImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(64, 72, 80)
    open val largeImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(100, 125, 150)
    open val extraImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(200, 225, 250)
    open val progressHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(4, 5, 6)
    open val sliderHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(8, 10, 12)
    open val dotHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 12, 14)
    open val refreshHeaderHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(70, 75, 80)
    open val refreshFooterHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(50, 55, 60)
    open val microCellWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(75, 90, 115)
    open val cellWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(150, 180, 200)
    open val cardWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(300, 320, 350)
    open val dialogWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(300, 400, 500)
    open val minDialogContentHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(50, 45, 40)
    open val maxDialogContentHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(260, 280, 300)
    open val sheetWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(360, 400, 450)
    open val panelWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(360, 380, 400)
}

@Stable
open class BaseCustomPadding {
    open val zeroSpace: Dp @Composable @ReadOnlyComposable get() = 0.dp
    open val littleSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    open val equalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(7.5, 9, 10.5)
    open val horizontalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 12, 14)
    open val verticalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(5, 6, 7)
    open val equalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(11.25, 13.125, 15)
    open val horizontalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(15, 17.5, 20)
    open val verticalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(7.5, 8.75, 10)
    open val innerIconSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    open val zeroValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(0.dp)
    open val littleValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = littleSpace, vertical = littleSpace * 0.8f)
    open val equalValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = equalSpace, vertical = equalSpace)
    open val value: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = horizontalSpace, vertical = verticalSpace)
    open val equalExtraValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = equalExtraSpace, vertical = equalExtraSpace)
    open val extraValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = horizontalExtraSpace, vertical = verticalExtraSpace)
    open val fabSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(16, 20, 24)
    open val sheetValue: PaddingValues @Composable @ReadOnlyComposable get() = when (LocalDevice.current.type) {
        Device.Type.PORTRAIT -> PaddingValues(horizontal = horizontalExtraSpace, vertical = verticalExtraSpace)
        else -> PaddingValues(top = horizontalExtraSpace, bottom = horizontalExtraSpace, end = horizontalExtraSpace)
    }
    open val cardSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(32, 40, 48)
    open val cardValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(cardSpace)
}

@Stable
open class BaseCustomBorder {
    open val small: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
    open val medium: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2, 2.5, 3)
    open val large: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(3, 4, 5)
}

@Stable
open class BaseCustomShadow {
    open val icon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    open val item: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
    open val miniSurface: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2, 2.25, 2.5)
    open val surface: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(4, 4.5, 5)
    open val card: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 15, 20)
    open val tonal: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
}

@Stable
open class BaseCustomTheme {
    open val typography = BaseCustomTypography()
    open val colorScheme = BaseCustomColorScheme()
    open val size = BaseCustomSize()
    open val padding = BaseCustomPadding()
    open val border = BaseCustomBorder()
    open val shadow = BaseCustomShadow()
}

val LocalCustomTheme = localComposition { BaseCustomTheme() }

val CustomTheme @Composable @ReadOnlyComposable get() = LocalCustomTheme.current