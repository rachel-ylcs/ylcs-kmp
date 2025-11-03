package love.yinlin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import love.yinlin.compose.*
import love.yinlin.extension.Reference
import org.jetbrains.compose.resources.FontResource

@Stable
abstract class Application<out A : Application<A>>(
    private val self: Reference<A?>,
    delegate: PlatformContextDelegate,
) : Service() {
    protected open val themeMode: ThemeMode = ThemeMode.SYSTEM
    protected open val fontScale: Float = 1f
    protected open val mainFontResource: FontResource? = null
    protected open val colorSystem: ColorSystem = DefaultColorSystem
    protected open val shapeSystem: ShapeSystem = DefaultShapeSystem
    protected open val textSystem: TextSystem = DefaultTextSystem
    protected open val customTheme: BaseCustomTheme? = null
    protected open val localProvider: Array<ProvidedValue<*>> = emptyArray()

    protected open fun onCreate() { }
    protected open fun onCreateDelay() { }
    protected open fun onDestroy() { }
    protected open fun onDestroyDelay() { }

    @Composable
    abstract fun Content()

    val context: Context = Context(delegate)

    @Composable
    fun Layout(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable () -> Unit = { Content() }) {
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            val device = remember(maxWidth, maxHeight) { Device(maxWidth, maxHeight) }

            val isDarkMode = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val providers = buildList {
                add(LocalDevice provides device)
                add(LocalDarkMode provides isDarkMode)
                add(LocalMainFontResource provides mainFontResource)
                customTheme?.let { add(LocalCustomTheme provides it) }
            }
            CompositionLocalProvider(*providers.toTypedArray()) {
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
                        LocalDensity provides Density(LocalDensity.current.density, fontScale),
                        *localProvider
                    ) {
                        content()
                    }
                }
            }
        }
    }

    internal fun initialize(delay: Boolean) {
        if (self.value == null) {
            @Suppress("UNCHECKED_CAST")
            self.value = this as? A
        }
        initService(context, delay)
        if (delay) onCreateDelay()
        else onCreate()
    }

    internal fun destroy(delay: Boolean) {
        destroyService(context, delay)
        if (delay) onDestroyDelay()
        else onDestroy()
    }
}