package love.yinlin.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class ModeColor(
    val light: Color,
    val dark: Color
)

infix fun Color.and(other: Color): ModeColor = ModeColor(this, other)

fun Boolean.select(ifTrue: Color, ifFalse: Color): Color = if (this) ifTrue else ifFalse

@Stable
data class ColorSystem(
    val primary: ModeColor,
    val onPrimary: ModeColor,
    val primaryContainer: ModeColor,
    val onPrimaryContainer: ModeColor,
    val secondary: ModeColor,
    val onSecondary: ModeColor,
    val secondaryContainer: ModeColor,
    val onSecondaryContainer: ModeColor,
    val tertiary: ModeColor,
    val onTertiary: ModeColor,
    val tertiaryContainer: ModeColor,
    val onTertiaryContainer: ModeColor,
    val background: ModeColor,
    val onBackground: ModeColor,
    val surface: ModeColor,
    val onSurface: ModeColor,
    val onSurfaceVariant: ModeColor,
    val error: ModeColor,
    val onError: ModeColor,
    val scrim: ModeColor,
) {
    fun toColorScheme(isDarkMode: Boolean): ColorScheme = if (!isDarkMode) lightColorScheme(
        primary = primary.light,
        onPrimary = onPrimary.light,
        primaryContainer = primaryContainer.light,
        onPrimaryContainer = onPrimaryContainer.light,
        secondary = secondary.light,
        onSecondary = onSecondary.light,
        secondaryContainer = secondaryContainer.light,
        onSecondaryContainer = onSecondaryContainer.light,
        tertiary = tertiary.light,
        onTertiary = onTertiary.light,
        tertiaryContainer = tertiaryContainer.light,
        onTertiaryContainer = onTertiaryContainer.light,
        background = background.light,
        onBackground = onBackground.light,
        surface = surface.light,
        onSurface = onSurface.light,
        onSurfaceVariant = onSurfaceVariant.light,
        error = error.light,
        onError = onError.light,
        scrim = scrim.light,
    ) else darkColorScheme(
        primary = primary.dark,
        onPrimary = onPrimary.dark,
        primaryContainer = primaryContainer.dark,
        onPrimaryContainer = onPrimaryContainer.dark,
        secondary = secondary.dark,
        onSecondary = onSecondary.dark,
        secondaryContainer = secondaryContainer.dark,
        onSecondaryContainer = onSecondaryContainer.dark,
        tertiary = tertiary.dark,
        onTertiary = onTertiary.dark,
        tertiaryContainer = tertiaryContainer.dark,
        onTertiaryContainer = onTertiaryContainer.dark,
        background = background.dark,
        onBackground = onBackground.dark,
        surface = surface.dark,
        onSurface = onSurface.dark,
        onSurfaceVariant = onSurfaceVariant.dark,
        error = error.dark,
        onError = onError.dark,
        scrim = scrim.dark,
    )
}

val DefaultColorSystem = ColorSystem(
    primary = Colors.Steel4 and Color(0xffb0d5de),
    onPrimary = Colors.Ghost and Colors.Ghost,
    primaryContainer = Colors.Steel6 and Color(0xff7da1aa),
    onPrimaryContainer = Colors.Ghost and Colors.Ghost,
    secondary = Color(0xff76c1c6) and Color(0xff9ac84b),
    onSecondary = Colors.Ghost and Colors.Ghost,
    secondaryContainer = Color(0xff1c8d95) and Color(0xff608c46),
    onSecondaryContainer = Colors.Ghost and Colors.Ghost,
    tertiary = Color(0xffef91a1) and Color(0xffd6c8ff),
    onTertiary = Colors.Ghost and Colors.Ghost,
    tertiaryContainer = Color(0xffc48b92) and Color(0xff7a89ce),
    onTertiaryContainer = Colors.Ghost and Colors.Ghost,
    background = Colors.Ghost and Colors.Dark,
    onBackground = Colors.Black and Colors.White,
    surface = Colors.Gray2 and Colors.Gray8,
    onSurface = Colors.Black and Colors.White,
    onSurfaceVariant = Colors.Gray5 and Colors.Gray4,
    error = Colors.Red5 and Colors.Red4,
    onError = Colors.White and Colors.Ghost,
    scrim = Colors.Dark and Colors.Black,
)