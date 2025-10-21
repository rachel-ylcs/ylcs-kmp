package love.yinlin.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
interface BaseCustomTypography {
    val bodyExtraSmall: TextStyle @Composable get() = ModeText(false, 10, 11, 12).style
    val displayExtraLarge: TextStyle @Composable get() = ModeText(false, 32, 34, 36).style
    val rhymeDisplay: TextStyle @Composable get() = ModeText(false, 100, 100, 100).style
}

@Stable
interface BaseCustomColorScheme {
    val warning: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Yellow4, Colors.Yellow5)
    val onWarning: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Dark, Colors.White)
    val backgroundVariant: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Dark, Colors.Ghost)
    val onBackgroundVariant: Color @Composable @ReadOnlyComposable get() = LocalDarkMode.current.select(Colors.Ghost, Colors.Dark)
}

@Stable
interface BaseCustomSize {
    val little: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    val microIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(18, 20, 22)
    val icon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(22, 24, 26)
    val mediumIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(26, 28, 30)
    val largeIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(30, 32, 34)
    val extraIcon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(34, 36, 38)
    val smallInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(36, 40, 44)
    val mediumInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(44, 50, 56)
    val largeInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(56, 64, 72)
    val extraInput: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(72, 80, 88)
    val sliderWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(90, 96, 102)
    val fab: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(40, 44, 48)
    val microImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(32, 40, 48)
    val image: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(48, 56, 64)
    val mediumImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(64, 72, 80)
    val largeImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(100, 125, 150)
    val extraImage: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(200, 225, 250)
    val progressHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(4, 5, 6)
    val sliderHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(8, 10, 12)
    val dotHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 12, 14)
    val refreshHeaderHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(70, 75, 80)
    val refreshFooterHeight: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(50, 55, 60)
    val microCellWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(75, 90, 115)
    val cellWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(150, 180, 200)
    val cardWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(300, 320, 350)
    val dialogWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(300, 400, 500)
    val sheetWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(360, 400, 450)
    val panelWidth: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(360, 380, 400)
}

@Stable
interface BaseCustomPadding {
    val zeroSpace: Dp @Composable @ReadOnlyComposable get() = 0.dp
    val littleSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    val equalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(7.5, 9, 10.5)
    val horizontalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 12, 14)
    val verticalSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(5, 6, 7)
    val equalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(11.25, 13.125, 15)
    val horizontalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(15, 17.5, 20)
    val verticalExtraSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(7.5, 8.75, 10)
    val innerIconSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    val zeroValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(0.dp)
    val littleValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = littleSpace, vertical = littleSpace * 0.8f)
    val equalValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = equalSpace, vertical = equalSpace)
    val value: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = horizontalSpace, vertical = verticalSpace)
    val equalExtraValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = equalExtraSpace, vertical = equalExtraSpace)
    val extraValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(horizontal = horizontalExtraSpace, vertical = verticalExtraSpace)
    val fabSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(16, 20, 24)
    val sheetValue: PaddingValues @Composable @ReadOnlyComposable get() = when (LocalDevice.current.type) {
        Device.Type.PORTRAIT -> PaddingValues(horizontal = horizontalExtraSpace, vertical = verticalExtraSpace)
        else -> PaddingValues(top = horizontalExtraSpace, bottom = horizontalExtraSpace, end = horizontalExtraSpace)
    }
    val cardSpace: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(32, 40, 48)
    val cardValue: PaddingValues @Composable @ReadOnlyComposable get() = PaddingValues(cardSpace)
}

@Stable
interface BaseCustomBorder {
    val small: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
    val medium: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2, 2.5, 3)
    val large: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(3, 4, 5)
}

@Stable
interface BaseCustomShadow {
    val icon: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2.5, 3, 3.5)
    val item: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
    val miniSurface: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(2, 2.25, 2.5)
    val surface: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(4, 4.5, 5)
    val card: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(10, 15, 20)
    val tonal: Dp @Composable @ReadOnlyComposable get() = LocalDevice.current.size.select(1, 1.5, 2)
}

@Stable
interface BaseCustomTheme {
    val typography: BaseCustomTypography get() = object : BaseCustomTypography { }
    val colorScheme: BaseCustomColorScheme get() = object : BaseCustomColorScheme { }
    val size: BaseCustomSize get() = object : BaseCustomSize { }
    val padding: BaseCustomPadding get() = object : BaseCustomPadding { }
    val border: BaseCustomBorder get() = object : BaseCustomBorder { }
    val shadow: BaseCustomShadow get() = object : BaseCustomShadow { }
}

val LocalCustomTheme = localComposition<BaseCustomTheme> { object : BaseCustomTheme { } }

val CustomTheme @Composable @ReadOnlyComposable get() = LocalCustomTheme.current