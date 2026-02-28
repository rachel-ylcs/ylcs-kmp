package love.yinlin.compose

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import love.yinlin.compose.extension.localComposition
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.staticLocalComposition
import org.jetbrains.compose.resources.FontResource

internal val LocalAnimationTheme = staticLocalComposition { AnimationTheme.Default }
internal val LocalBorderTheme = staticLocalComposition { GeometryTheme.Default.border }
internal val LocalColorTheme = staticLocalComposition { ColorSystem.Default.light }
internal val LocalDarkMode = staticLocalComposition { false }
internal val LocalMainFontResource = staticLocalComposition<FontResource?>()
internal val LocalPaddingTheme = staticLocalComposition { GeometryTheme.Default.padding }
internal val LocalShadowTheme = staticLocalComposition { GeometryTheme.Default.shadow }
internal val LocalShapeTheme = staticLocalComposition { ShapeTheme.Default }
internal val LocalSizeTheme = staticLocalComposition { GeometryTheme.Default.size }
internal val LocalToolingTheme = staticLocalComposition { ToolingTheme.Default }
internal val LocalTypographyTheme = staticLocalComposition { TypographyTheme.Default }
internal val LocalValueTheme = staticLocalComposition { ValueTheme.Default }

val LocalColor = localComposition { ColorSystem.Default.light.onBackground }
val LocalColorVariant = localComposition { ColorSystem.Default.light.onBackgroundVariant }
val LocalStyle = localComposition { TypographyTheme.Default.default }

@Composable
fun Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = 1f,
    mainFontResource: FontResource? = null,
    background: Color? = null,
    colorSystem: ColorSystem = ColorSystem.Default,
    typographyTheme: TypographyTheme = TypographyTheme.Default,
    shapeTheme: ShapeTheme = ShapeTheme.Default,
    geometryTheme: GeometryTheme = GeometryTheme.Default,
    animationTheme: AnimationTheme = AnimationTheme.Default,
    toolingTheme: ToolingTheme = ToolingTheme.Default,
    valueTheme: ValueTheme = ValueTheme.Default,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        val isDarkMode = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        val windowInfo = LocalWindowInfo.current
        val device by rememberDerivedState {
            val containerSize = windowInfo.containerDpSize
            Device(containerSize.width, containerSize.height)
        }

        val densityInfo = LocalDensity.current
        val density = remember(densityInfo, fontScale) { Density(densityInfo.density, fontScale) }

        val colorTheme = if (isDarkMode) colorSystem.dark else colorSystem.light

        CompositionLocalProvider(
            LocalDarkMode provides isDarkMode,
            LocalDevice provides device,
            LocalMainFontResource provides mainFontResource,
            LocalColorTheme provides colorTheme,
            LocalDensity provides density,
            LocalIndication provides Ripple,
        ) {
            val font = mainFont()

            val currentTypographyTheme = remember(font, typographyTheme) { typographyTheme.updateFont(font) }

            val primaryColor = colorTheme.primary

            val selectionColors = remember(primaryColor) {
                TextSelectionColors(primaryColor, primaryColor.copy(alpha = 0.4f))
            }

            CompositionLocalProvider(
                LocalTypographyTheme provides currentTypographyTheme,
                LocalShapeTheme provides shapeTheme,
                LocalSizeTheme provides geometryTheme.size,
                LocalPaddingTheme provides geometryTheme.padding,
                LocalBorderTheme provides geometryTheme.border,
                LocalShadowTheme provides geometryTheme.shadow,
                LocalAnimationTheme provides animationTheme,
                LocalToolingTheme provides toolingTheme,
                LocalValueTheme provides valueTheme,
                LocalColor provides colorTheme.onBackground,
                LocalColorVariant provides colorTheme.onBackgroundVariant,
                LocalStyle provides currentTypographyTheme.default,
                LocalTextSelectionColors provides selectionColors,
            ) {
                Box(modifier = Modifier.background(background ?: colorTheme.background)) {
                    content()
                }
            }
        }
    }
}

@Stable
object Theme {
    val animation: AnimationTheme @Composable @ReadOnlyComposable get() = LocalAnimationTheme.current
    val border: BorderTheme @Composable @ReadOnlyComposable get() = LocalBorderTheme.current
    val color: ColorTheme @Composable @ReadOnlyComposable get() = LocalColorTheme.current
    val darkMode: Boolean @Composable @ReadOnlyComposable get() = LocalDarkMode.current
    val padding: PaddingTheme @Composable @ReadOnlyComposable get() = LocalPaddingTheme.current
    val shadow: ShadowTheme @Composable @ReadOnlyComposable get() = LocalShadowTheme.current
    val shape: ShapeTheme @Composable @ReadOnlyComposable get() = LocalShapeTheme.current
    val size: SizeTheme @Composable @ReadOnlyComposable get() = LocalSizeTheme.current
    val tool: ToolingTheme @Composable @ReadOnlyComposable get() = LocalToolingTheme.current
    val typography: TypographyTheme @Composable @ReadOnlyComposable get() = LocalTypographyTheme.current
    val value: ValueTheme @Composable @ReadOnlyComposable get() = LocalValueTheme.current
}