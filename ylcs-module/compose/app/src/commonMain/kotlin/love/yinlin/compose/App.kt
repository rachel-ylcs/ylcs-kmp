package love.yinlin.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.FontResource

@Composable
fun App(
    deviceFactory: (maxWidth: Dp, maxHeight: Dp) -> Device,
    themeMode: ThemeMode,
    fontScale: Float,
    mainFontResource: FontResource,
    colorSystem: ColorSystem = DefaultColorSystem,
    shapeSystem: ShapeSystem = DefaultShapeSystem,
    textSystem: TextSystem = DefaultTextSystem,
    customTheme: BaseCustomTheme = LocalCustomTheme.current,
    modifier: Modifier,
    alignment: Alignment = Alignment.TopStart,
    content: @Composable (maxWidth: Dp, maxHeight: Dp) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        val device = remember(maxWidth, maxHeight, deviceFactory) { deviceFactory(maxWidth, maxHeight) }

        val isDarkMode = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        CompositionLocalProvider(
            LocalDevice provides device,
            LocalDarkMode provides isDarkMode,
            LocalMainFontResource provides mainFontResource,
            LocalCustomTheme provides customTheme,
        ) {
            val colorScheme = remember(isDarkMode, colorSystem) {
                colorSystem.toColorScheme(isDarkMode)
            }

            val shapes = remember(device, shapeSystem) {
                shapeSystem.toShapes(device.size)
            }

            val font = mainFont()

            val typography = remember(device, font, textSystem) {
                textSystem.toTypography(font, device.size)
            }

            MaterialTheme(
                colorScheme = colorScheme,
                shapes = shapes,
                typography = typography
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalDensity provides Density(LocalDensity.current.density, fontScale)
                ) {
                    content(maxWidth, maxHeight)
                }
            }
        }
    }
}