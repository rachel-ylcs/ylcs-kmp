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
	val Gray8 = Color(0xff334155)
	val Red1 = Color(0xfffecaca)
	val Red2 = Color(0xfffca5a5)
	val Red3 = Color(0xfff87171)
	val Red4 = Color(0xffef4444)
	val Red5 = Color(0xffdc2626)
	val Red6 = Color(0xffb91c1c)
	val Red7 = Color(0xff991b1b)
	val Red8 = Color(0xff7f1d1d)
	val Orange1 = Color(0xfffed7aa)
	val Orange2 = Color(0xfffdba74)
	val Orange3 = Color(0xfffb923c)
	val Orange4 = Color(0xfff97316)
	val Orange5 = Color(0xffea580c)
	val Orange6 = Color(0xffc2410c)
	val Orange7 = Color(0xff9a3412)
	val Orange8 = Color(0xff7c2d12)
	val Yellow1 = Color(0xfffefce8)
	val Yellow2 = Color(0xfffef9c3)
	val Yellow3 = Color(0xfffef08a)
	val Yellow4 = Color(0xfffde047)
	val Yellow5 = Color(0xfffacc15)
	val Yellow6 = Color(0xffeab308)
	val Yellow7 = Color(0xffca8a04)
	val Yellow8 = Color(0xffa16207)
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
	val Blue1 = Color(0xffbfdbfe)
	val Blue2 = Color(0xff93c5fd)
	val Blue3 = Color(0xff60a5fa)
	val Blue4 = Color(0xff3b82f6)
	val Blue5 = Color(0xff2563eb)
	val Blue6 = Color(0xff1d4ed8)
	val Blue7 = Color(0xff1e40af)
	val Blue8 = Color(0xff1e3a8a)
	val Purple1 = Color(0xffe9d5ff)
	val Purple2 = Color(0xffd8b4fe)
	val Purple3 = Color(0xffc084fc)
	val Purple4 = Color(0xffa855f7)
	val Purple5 = Color(0xff9333ea)
	val Purple6 = Color(0xff7e22ce)
	val Purple7 = Color(0xff6b21a8)
	val Purple8 = Color(0xff581c87)
	val Pink1 = Color(0xfffbcfe8)
	val Pink2 = Color(0xfff9a8d4)
	val Pink3 = Color(0xfff472b6)
	val Pink4 = Color(0xffec4899)
	val Pink5 = Color(0xffdb2777)
	val Pink6 = Color(0xffbe185d)
	val Pink7 = Color(0xff9d174d)
	val Pink8 = Color(0xff831843)
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
	val fade: Color @Composable get() = if (app.isDarkMode) Colors.Gray4 else Colors.Gray5
	val warning: Color @Composable get() = if (app.isDarkMode) Colors.Yellow4 else Colors.Red4
	val primaryGradient: List<Color> @Composable get() =
		if (app.isDarkMode) listOf(Colors.Red4, Colors.Orange4, Colors.Red4)
		else listOf(Colors.Steel4, Colors.Purple4, Colors.Steel4)
}

private val LightColorScheme = lightColorScheme(
	primary = Colors.Steel4,
	onPrimary = Colors.White,
	primaryContainer = Colors.Steel2,
	onPrimaryContainer = Colors.White,
	secondary = Colors.Purple4,
	onSecondary = Colors.White,
	secondaryContainer = Colors.Purple2,
	onSecondaryContainer = Colors.White,
	tertiary = Colors.Pink4,
	onTertiary = Colors.White,
	tertiaryContainer = Colors.Pink2,
	onTertiaryContainer = Colors.White,
	background = Colors.Ghost,
	onBackground = Colors.Black,
	surface = Colors.Gray2,
	onSurface = Colors.Black,
	onSurfaceVariant = Colors.Gray7,
	surfaceContainer = Colors.Gray1,
	error = Colors.Red4,
	onError = Colors.White
)

private val DarkColorScheme = darkColorScheme(
	primary = Colors.Red4,
	onPrimary = Colors.Black,
	primaryContainer = Colors.Red6,
	onPrimaryContainer = Colors.Black,
	secondary = Colors.Green4,
	onSecondary = Colors.Black,
	secondaryContainer = Colors.Green6,
	onSecondaryContainer = Colors.Black,
	tertiary = Colors.Orange4,
	onTertiary = Colors.Black,
	tertiaryContainer = Colors.Orange6,
	onTertiaryContainer = Colors.Black,
	background = Colors.Dark,
	onBackground = Colors.White,
	surface = Colors.Gray7,
	onSurface = Colors.White,
	onSurfaceVariant = Colors.Gray2,
	surfaceContainer = Colors.Gray8,
	error = Colors.Yellow4,
	onError = Colors.Black
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
		colorScheme = if (darkMode && !Local.Client.ALWAYS_LIGHT_MODE) DarkColorScheme else LightColorScheme,
		shapes = RachelShapes(),
		typography = RachelTypography(),
		content = content
	)
}