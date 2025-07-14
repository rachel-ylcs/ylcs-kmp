package love.yinlin.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk
import org.jetbrains.compose.resources.Font

@Stable
object Colors {
	val Unspecified = Color.Unspecified
	val Transparent = Color(0x00000000L)
	val White = Color(0xffffffff)
	val Ghost = Color(0xfff8f8ff)
	val Black = Color(0xff000000)
	val Dark = Color(0xff181818)
	val Gray1 = Color(0xfff8fafc)
	val Gray2 = Color(0xfff1f5f9)
	val Gray3 = Color(0xffe2e8f0)
	val Gray4 = Color(0xffcbd5e1)
	val Gray5 = Color(0xff94a3b8)
	val Gray6 = Color(0xff64748b)
	val Gray7 = Color(0xff475569)
	val Gray8 = Color(0xff262626)
	val Red = Color(255, 0, 0)
	val Red1 = Color(255, 226, 226)
	val Red2 = Color(255, 201, 201)
	val Red3 = Color(255, 162, 162)
	val Red4 = Color(255, 100, 103)
	val Red5 = Color(251, 44, 54)
	val Red6 = Color(231, 0, 11)
	val Red7 = Color(193, 0, 7)
	val Red8 = Color(159, 7, 18)
	val Orange1 = Color(255, 237, 212)
	val Orange2 = Color(255, 214, 167)
	val Orange3 = Color(255, 184, 106)
	val Orange4 = Color(255, 137, 4)
	val Orange5 = Color(255, 105, 0)
	val Orange6 = Color(245, 73, 0)
	val Orange7 = Color(202, 53, 0)
	val Orange8 = Color(159, 45, 0)
	val Yellow = Color(255, 255, 0)
	val Yellow1 = Color(254, 249, 194)
	val Yellow2 = Color(255, 240, 133)
	val Yellow3 = Color(255, 223, 32)
	val Yellow4 = Color(253, 199, 0)
	val Yellow5 = Color(240, 177, 0)
	val Yellow6 = Color(208, 135, 0)
	val Yellow7 = Color(166, 95, 0)
	val Yellow8 = Color(137, 75, 0)
	val Green = Color(0, 255, 0)
	val Green1 = Color(0xffbbf7d0)
	val Green2 = Color(0xff86efac)
	val Green3 = Color(0xff4ade80)
	val Green4 = Color(0xff22c55e)
	val Green5 = Color(0xff16a34a)
	val Green6 = Color(0xff15803d)
	val Green7 = Color(0xff166534)
	val Green8 = Color(0xff14532d)
	val Cyan = Color(0, 255, 255)
	val Cyan1 = Color(0xffa5f3fc)
	val Cyan2 = Color(0xff67e8f9)
	val Cyan3 = Color(0xff22d3ee)
	val Cyan4 = Color(0xff06b6d4)
	val Cyan5 = Color(0xff0891b2)
	val Cyan6 = Color(0xff0e7490)
	val Cyan7 = Color(0xff155e75)
	val Cyan8 = Color(0xff164e63)
	val Blue = Color(0xff0000ff)
	val Blue1 = Color(219, 234, 254)
	val Blue2 = Color(190, 219, 255)
	val Blue3 = Color(142, 197, 255)
	val Blue4 = Color(80, 162, 255)
	val Blue5 = Color(43, 127, 255)
	val Blue6 = Color(21, 93, 252)
	val Blue7 = Color(20, 71, 230)
	val Blue8 = Color(25, 60, 184)
	val Fuchsia = Color(255, 0, 255)
	val Purple1 = Color(237, 233, 254)
	val Purple2 = Color(221, 214, 255)
	val Purple3 = Color(218, 178, 255)
	val Purple4 = Color(194, 122, 255)
	val Purple5 = Color(173, 70, 255)
	val Purple6 = Color(152, 16, 250)
	val Purple7 = Color(130, 0, 219)
	val Purple8 = Color(110, 17, 176)
	val Pink1 = Color(252, 231, 243)
	val Pink2 = Color(252, 206, 232)
	val Pink3 = Color(253, 165, 213)
	val Pink4 = Color(251, 100, 182)
	val Pink5 = Color(246, 51, 154)
	val Pink6 = Color(230, 0, 118)
	val Pink7 = Color(198, 0, 92)
	val Pink8 = Color(163, 0, 76)
	val Steel1 = Color(0xffd4e6f1)
	val Steel2 = Color(0xffa9cce3)
	val Steel3 = Color(0xff7fb3d5)
	val Steel4 = Color(0xff4682b4)
	val Steel5 = Color(0xff2980b9)
	val Steel6 = Color(0xff2471a3)
	val Steel7 = Color(0xff1f618d)
	val Steel8 = Color(0xff1a5276)

	fun from(value: Int) = Color(value)
	fun from(value: Long) = Color(value)
	fun from(value: ULong) = Color(value)
}

@Stable
object ThemeColor {
	val warning: Color @Composable get() = if (LocalDarkMode.current) Colors.Yellow4 else Colors.Yellow5
	val onWarning: Color @Composable get() = if (LocalDarkMode.current) Colors.Dark else Colors.White
	val backgroundVariant: Color @Composable get() = if (LocalDarkMode.current) Colors.Dark else Colors.Ghost
	val onBackgroundVariant: Color @Composable get() = if (LocalDarkMode.current) Colors.Ghost else Colors.Dark
}

val LightColorScheme = lightColorScheme(
	primary = Colors.Steel4,
	onPrimary = Colors.Ghost,
	primaryContainer = Colors.Steel6,
	onPrimaryContainer = Colors.Ghost,
	secondary = Color(0xff76c1c6),
	onSecondary = Colors.Ghost,
	secondaryContainer = Color(0xff1c8d95),
	onSecondaryContainer = Colors.Ghost,
	tertiary = Color(0xffef91a1),
	onTertiary = Colors.Ghost,
	tertiaryContainer = Color(0xffc48b92),
	onTertiaryContainer = Colors.Ghost,
	background = Colors.Ghost,
	onBackground = Colors.Black,
	surface = Colors.Gray2,
	onSurface = Colors.Black,
	onSurfaceVariant = Colors.Gray5,
	error = Colors.Red5,
	onError = Colors.White,
	scrim = Colors.Dark
)

val DarkColorScheme = darkColorScheme(
	primary = Color(0xffb0d5de),
	onPrimary = Colors.Ghost,
	primaryContainer = Color(0xff7da1aa),
	onPrimaryContainer = Colors.Ghost,
	secondary = Color(0xff9ac84b),
	onSecondary = Colors.Ghost,
	secondaryContainer = Color(0xff608c46),
	onSecondaryContainer = Colors.Ghost,
	tertiary = Color(0xffd6c8ff),
	onTertiary = Colors.Ghost,
	tertiaryContainer = Color(0xff7a89ce),
	onTertiaryContainer = Colors.Ghost,
	background = Colors.Dark,
	onBackground = Colors.White,
	surface = Colors.Gray8,
	onSurface = Colors.White,
	onSurfaceVariant = Colors.Gray4,
	error = Colors.Red4,
	onError = Colors.Ghost,
	scrim = Colors.Black
)

@Composable
fun rachelColorScheme(isDarkMode: Boolean): ColorScheme = remember(isDarkMode) {
	if (isDarkMode) DarkColorScheme else LightColorScheme
}

@Composable
fun rachelShapes(device: Device): Shapes = remember(device) { when (device.size) {
    Device.Size.SMALL -> Shapes(
		extraSmall = RoundedCornerShape(3.dp),
		small = RoundedCornerShape(5.dp),
		medium = RoundedCornerShape(7.dp),
		large = RoundedCornerShape(9.dp),
		extraLarge = RoundedCornerShape(11.dp)
	)
    Device.Size.MEDIUM -> Shapes(
		extraSmall = RoundedCornerShape(3.5.dp),
		small = RoundedCornerShape(5.5.dp),
		medium = RoundedCornerShape(7.5.dp),
		large = RoundedCornerShape(9.5.dp),
		extraLarge = RoundedCornerShape(11.5.dp)
	)
    Device.Size.LARGE -> Shapes(
		extraSmall = RoundedCornerShape(4.dp),
		small = RoundedCornerShape(6.dp),
		medium = RoundedCornerShape(8.dp),
		large = RoundedCornerShape(10.dp),
		extraLarge = RoundedCornerShape(12.dp)
	)
} }

private fun baseTextStyle(
	font: Font,
	size: TextUnit,
	isBold: Boolean = false
): TextStyle = TextStyle(
	fontFamily = FontFamily(font),
	fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Light,
	fontSize = size,
	lineHeight = size * 1.5f,
	letterSpacing = size / 16f
)

@Composable
private fun rachelFont(): Font = Font(Res.font.xwwk)

@Composable
private fun rachelTextStyle(
	size: TextUnit,
	isBold: Boolean = false
): TextStyle {
	val font = rachelFont()
	return remember(font, size, isBold) { baseTextStyle(font, size, isBold) }
}

@Composable
fun rachelTypography(device: Device): Typography {
	val font = rachelFont()
	return remember(device, font) { when (device.size) {
        Device.Size.SMALL -> Typography(
			displayLarge = baseTextStyle(font, 28.sp, true),
			displayMedium = baseTextStyle(font, 24.sp, true),
			displaySmall = baseTextStyle(font, 20.sp, true),
			headlineLarge = baseTextStyle(font, 28.sp, false),
			headlineMedium = baseTextStyle(font, 24.sp, false),
			headlineSmall = baseTextStyle(font, 20.sp, false),
			titleLarge = baseTextStyle(font, 17.sp, true),
			titleMedium = baseTextStyle(font, 15.sp, true),
			titleSmall = baseTextStyle(font, 13.sp, true),
			bodyLarge = baseTextStyle(font, 16.sp, false),
			bodyMedium = baseTextStyle(font, 14.sp, false),
			bodySmall = baseTextStyle(font, 12.sp, false),
			labelLarge = baseTextStyle(font, 16.sp, true),
			labelMedium = baseTextStyle(font, 14.sp, true),
			labelSmall = baseTextStyle(font, 12.sp, true),
		)
        Device.Size.MEDIUM -> Typography(
			displayLarge = baseTextStyle(font, 30.sp, true),
			displayMedium = baseTextStyle(font, 26.sp, true),
			displaySmall = baseTextStyle(font, 22.sp, true),
			headlineLarge = baseTextStyle(font, 30.sp, false),
			headlineMedium = baseTextStyle(font, 26.sp, false),
			headlineSmall = baseTextStyle(font, 22.sp, false),
			titleLarge = baseTextStyle(font, 18.sp, true),
			titleMedium = baseTextStyle(font, 16.sp, true),
			titleSmall = baseTextStyle(font, 14.sp, true),
			bodyLarge = baseTextStyle(font, 17.sp, false),
			bodyMedium = baseTextStyle(font, 15.sp, false),
			bodySmall = baseTextStyle(font, 13.sp, false),
			labelLarge = baseTextStyle(font, 17.sp, true),
			labelMedium = baseTextStyle(font, 15.sp, true),
			labelSmall = baseTextStyle(font, 13.sp, true),
		)
        Device.Size.LARGE -> Typography(
			displayLarge = baseTextStyle(font, 32.sp, true),
			displayMedium = baseTextStyle(font, 28.sp, true),
			displaySmall = baseTextStyle(font, 24.sp, true),
			headlineLarge = baseTextStyle(font, 32.sp, false),
			headlineMedium = baseTextStyle(font, 28.sp, false),
			headlineSmall = baseTextStyle(font, 24.sp, false),
			titleLarge = baseTextStyle(font, 19.sp, true),
			titleMedium = baseTextStyle(font, 17.sp, true),
			titleSmall = baseTextStyle(font, 15.sp, true),
			bodyLarge = baseTextStyle(font, 18.sp, false),
			bodyMedium = baseTextStyle(font, 16.sp, false),
			bodySmall = baseTextStyle(font, 14.sp, false),
			labelLarge = baseTextStyle(font, 18.sp, true),
			labelMedium = baseTextStyle(font, 16.sp, true),
			labelSmall = baseTextStyle(font, 14.sp, true),
		)
    } }
}

@Stable
object ThemeStyle {
	val bodyExtraSmall: TextStyle @Composable get() = when (LocalDevice.current.size) {
        Device.Size.SMALL -> rachelTextStyle(10.sp, false)
        Device.Size.MEDIUM -> rachelTextStyle(11.sp, false)
        Device.Size.LARGE -> rachelTextStyle(12.sp, false)
	}
	val DisplayExtraLarge: TextStyle @Composable get() = when (LocalDevice.current.size) {
        Device.Size.SMALL -> rachelTextStyle(32.sp, true)
        Device.Size.MEDIUM -> rachelTextStyle(34.sp, true)
        Device.Size.LARGE -> rachelTextStyle(36.sp, true)
    }
}

@Stable
object ThemeValue {
	@Stable
	object Size {
		val Little: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.5.dp
			Device.Size.MEDIUM -> 3.dp
			Device.Size.LARGE -> 3.5.dp
		}
		val MicroIcon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 18.dp
			Device.Size.MEDIUM -> 20.dp
			Device.Size.LARGE -> 22.dp
		}
		val Icon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 22.dp
			Device.Size.MEDIUM -> 24.dp
			Device.Size.LARGE -> 26.dp
		}
		val MediumIcon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 26.dp
			Device.Size.MEDIUM -> 28.dp
			Device.Size.LARGE -> 30.dp
		}
		val LargeIcon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 30.dp
			Device.Size.MEDIUM -> 32.dp
			Device.Size.LARGE -> 34.dp
		}
		val ExtraIcon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 34.dp
			Device.Size.MEDIUM -> 36.dp
			Device.Size.LARGE -> 38.dp
		}
		val SmallInput: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 36.dp
			Device.Size.MEDIUM -> 40.dp
			Device.Size.LARGE -> 44.dp
		}
		val MediumInput: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 44.dp
			Device.Size.MEDIUM -> 50.dp
			Device.Size.LARGE -> 56.dp
		}
		val LargeInput: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 56.dp
			Device.Size.MEDIUM -> 64.dp
			Device.Size.LARGE -> 72.dp
		}
		val ExtraInput: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 72.dp
			Device.Size.MEDIUM -> 80.dp
			Device.Size.LARGE -> 88.dp
		}
        val SliderWidth: Dp @Composable get() = when (LocalDevice.current.size) {
            Device.Size.SMALL -> 90.dp
            Device.Size.MEDIUM -> 96.dp
            Device.Size.LARGE -> 102.dp
        }
		val FAB: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 40.dp
			Device.Size.MEDIUM -> 44.dp
			Device.Size.LARGE -> 48.dp
		}
		val MicroImage: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 32.dp
			Device.Size.MEDIUM -> 40.dp
			Device.Size.LARGE -> 48.dp
		}
		val Image: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 48.dp
			Device.Size.MEDIUM -> 56.dp
			Device.Size.LARGE -> 64.dp
		}
		val MediumImage: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 64.dp
			Device.Size.MEDIUM -> 72.dp
			Device.Size.LARGE -> 80.dp
		}
		val LargeImage: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 100.dp
			Device.Size.MEDIUM -> 125.dp
			Device.Size.LARGE -> 150.dp
		}
		val ExtraImage: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 200.dp
			Device.Size.MEDIUM -> 225.dp
			Device.Size.LARGE -> 250.dp
		}
		val ProgressHeight: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 4.dp
			Device.Size.MEDIUM -> 5.dp
			Device.Size.LARGE -> 6.dp
		}
		val SliderHeight: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 8.dp
			Device.Size.MEDIUM -> 10.dp
			Device.Size.LARGE -> 12.dp
		}
		val dotHeight: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 10.dp
			Device.Size.MEDIUM -> 12.dp
			Device.Size.LARGE -> 14.dp
		}
		val RefreshHeaderHeight: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 70.dp
			Device.Size.MEDIUM -> 75.dp
			Device.Size.LARGE -> 80.dp
		}
		val RefreshFooterHeight: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 50.dp
			Device.Size.MEDIUM -> 55.dp
			Device.Size.LARGE -> 60.dp
		}
		val MicroCellWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 75.dp
			Device.Size.MEDIUM -> 90.dp
			Device.Size.LARGE -> 115.dp
		}
		val CellWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 150.dp
			Device.Size.MEDIUM -> 180.dp
			Device.Size.LARGE -> 200.dp
		}
		val CardWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 300.dp
			Device.Size.MEDIUM -> 320.dp
			Device.Size.LARGE -> 350.dp
		}
		val DialogWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 300.dp
			Device.Size.MEDIUM -> 400.dp
			Device.Size.LARGE -> 500.dp
		}
		val SheetWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 360.dp
			Device.Size.MEDIUM -> 400.dp
			Device.Size.LARGE -> 450.dp
		}
		val PanelWidth: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 360.dp
			Device.Size.MEDIUM -> 380.dp
			Device.Size.LARGE -> 400.dp
		}
	}

	@Stable
	object Padding {
		val ZeroSpace: Dp = 0.dp
		val LittleSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.5.dp
			Device.Size.MEDIUM -> 3.dp
			Device.Size.LARGE -> 3.5.dp
		}
		val EqualSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 7.5.dp
			Device.Size.MEDIUM -> 9.dp
			Device.Size.LARGE -> 10.5.dp
		}
		val HorizontalSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 10.dp
			Device.Size.MEDIUM -> 12.dp
			Device.Size.LARGE -> 14.dp
		}
		val VerticalSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 5.dp
			Device.Size.MEDIUM -> 6.dp
			Device.Size.LARGE -> 7.dp
		}
		val EqualExtraSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 11.25.dp
			Device.Size.MEDIUM -> 13.125.dp
			Device.Size.LARGE -> 15.dp
		}
		val HorizontalExtraSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 15.dp
			Device.Size.MEDIUM -> 17.5.dp
			Device.Size.LARGE -> 20.dp
		}
		val VerticalExtraSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 7.5.dp
			Device.Size.MEDIUM -> 8.75.dp
			Device.Size.LARGE -> 10.dp
		}
		val InnerIcon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.5.dp
			Device.Size.MEDIUM -> 3.dp
			Device.Size.LARGE -> 3.5.dp
		}
		val ZeroValue: PaddingValues = PaddingValues(0.dp)
		val LittleValue: PaddingValues @Composable get() = PaddingValues(horizontal = LittleSpace, vertical = LittleSpace * 0.8f)
		val EqualValue: PaddingValues @Composable get() = PaddingValues(horizontal = EqualSpace, vertical = EqualSpace)
		val Value: PaddingValues @Composable get() = PaddingValues(horizontal = HorizontalSpace, vertical = VerticalSpace)
		val EqualExtraValue: PaddingValues @Composable get() = PaddingValues(horizontal = EqualExtraSpace, vertical = EqualExtraSpace)
		val ExtraValue: PaddingValues @Composable get() = PaddingValues(horizontal = HorizontalExtraSpace, vertical = VerticalExtraSpace)
		val FAB: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 16.dp
			Device.Size.MEDIUM -> 20.dp
			Device.Size.LARGE -> 24.dp
		}
		val SheetValue: PaddingValues @Composable get() = when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> PaddingValues(horizontal = HorizontalExtraSpace, vertical = VerticalExtraSpace)
			else -> PaddingValues(top = HorizontalExtraSpace, bottom = HorizontalExtraSpace, end = HorizontalExtraSpace)
		}
		val CardSpace: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 32.dp
			Device.Size.MEDIUM -> 40.dp
			Device.Size.LARGE -> 48.dp
		}
		val CardValue: PaddingValues @Composable get() = PaddingValues(CardSpace)
	}

	@Stable
	object Border {
		val Small: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 1.dp
			Device.Size.MEDIUM -> 1.5.dp
			Device.Size.LARGE -> 2.dp
		}
		val Medium: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.dp
			Device.Size.MEDIUM -> 2.5.dp
			Device.Size.LARGE -> 3.dp
		}
		val Large: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 3.dp
			Device.Size.MEDIUM -> 4.dp
			Device.Size.LARGE -> 5.dp
		}
	}

	@Stable
	object Shadow {
		val Icon: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.5.dp
			Device.Size.MEDIUM -> 3.dp
			Device.Size.LARGE -> 3.5.dp
		}
		val Item: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 1.dp
			Device.Size.MEDIUM -> 1.5.dp
			Device.Size.LARGE -> 2.dp
		}
		val MiniSurface: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 2.dp
			Device.Size.MEDIUM -> 2.25.dp
			Device.Size.LARGE -> 2.5.dp
		}
		val Surface: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 4.dp
			Device.Size.MEDIUM -> 4.5.dp
			Device.Size.LARGE -> 5.dp
		}
		val Card: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 10.dp
			Device.Size.MEDIUM -> 15.dp
			Device.Size.LARGE -> 20.dp
		}
		val Tonal: Dp @Composable get() = when (LocalDevice.current.size) {
			Device.Size.SMALL -> 1.dp
			Device.Size.MEDIUM -> 1.5.dp
			Device.Size.LARGE -> 2.dp
		}
	}
}