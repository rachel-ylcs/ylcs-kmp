package love.yinlin.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yinlin.Local
import love.yinlin.platform.app
import org.jetbrains.compose.resources.Font
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk

enum class ThemeMode {
	SYSTEM, LIGHT, DARK;

	val next: ThemeMode get() = when (this) {
		SYSTEM -> LIGHT
		LIGHT -> DARK
		DARK -> SYSTEM
	}
}

object Colors {
	val Unspecified = Color.Unspecified
	val Transparent = Color(0x00000000)
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
	val Yellow1 = Color(254, 249, 194)
	val Yellow2 = Color(255, 240, 133)
	val Yellow3 = Color(255, 223, 32)
	val Yellow4 = Color(253, 199, 0)
	val Yellow5 = Color(240, 177, 0)
	val Yellow6 = Color(208, 135, 0)
	val Yellow7 = Color(166, 95, 0)
	val Yellow8 = Color(137, 75, 0)
	val Green1 = Color(0xffbbf7d0)
	val Green2 = Color(0xff86efac)
	val Green3 = Color(0xff4ade80)
	val Green4 = Color(0xff22c55e)
	val Green5 = Color(0xff16a34a)
	val Green6 = Color(0xff15803d)
	val Green7 = Color(0xff166534)
	val Green8 = Color(0xff14532d)
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
}

object ThemeColor {
	val warning: Color @Composable get() = if (app.isDarkMode) Colors.Yellow4 else Colors.Yellow5
	val onWarning: Color @Composable get() = if (app.isDarkMode) Colors.Ghost else Colors.White
	val backgroundVariant: Color @Composable get() = if (app.isDarkMode) Colors.Dark else Colors.Ghost
	val onBackgroundVariant: Color @Composable get() = if (app.isDarkMode) Colors.Ghost else Colors.Dark
}

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
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
private fun RachelTextStyle(size: TextUnit, isBold: Boolean = false): TextStyle = TextStyle(
	fontFamily = FontFamily(Font(Res.font.xwwk)),
	fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Light,
	fontSize = size,
	lineHeight = size * 1.5f,
	letterSpacing = if (size > 16.sp) 1.sp else 0.5.sp
)

@Composable
private fun RachelTypography(): Typography = Typography(
	displayLarge = RachelTextStyle(26.sp, true),
	displayMedium = RachelTextStyle(22.sp, true),
	displaySmall = RachelTextStyle(18.sp, true),
	headlineLarge = RachelTextStyle(26.sp, false),
	headlineMedium = RachelTextStyle(22.sp, false),
	headlineSmall = RachelTextStyle(18.sp, false),
	titleLarge = RachelTextStyle(16.sp, true),
	titleMedium = RachelTextStyle(14.sp, true),
	titleSmall = RachelTextStyle(12.sp, true),
	bodyLarge = RachelTextStyle(14.sp, false),
	bodyMedium = RachelTextStyle(12.sp, false),
	bodySmall = RachelTextStyle(10.sp, false),
	labelLarge = RachelTextStyle(14.sp, true),
	labelMedium = RachelTextStyle(12.sp, true),
	labelSmall = RachelTextStyle(10.sp, true),
)

@Composable
private fun RachelShapes(): Shapes = Shapes(
	extraSmall = RoundedCornerShape(4.dp),
	small = RoundedCornerShape(6.dp),
	medium = RoundedCornerShape(8.dp),
	large = RoundedCornerShape(10.dp),
	extraLarge = RoundedCornerShape(12.dp)
)

@Suppress("SimplifyBooleanWithConstants")
@Composable
fun RachelTheme(darkMode: Boolean, content: @Composable () -> Unit) {
	MaterialTheme(
		colorScheme = if (darkMode || Local.Client.ALWAYS_DARK_MODE) DarkColorScheme else LightColorScheme,
		shapes = RachelShapes(),
		typography = RachelTypography(),
		content = content
	)
}