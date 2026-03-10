package love.yinlin.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import love.yinlin.extension.BaseLazyReference
import love.yinlin.foundation.Context
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.foundation.Service
import org.jetbrains.compose.resources.FontResource

@Stable
abstract class Application<out A : Application<A>>(
    private val self: BaseLazyReference<A>,
    delegate: PlatformContextDelegate,
) : Service() {
    protected open val themeMode: ThemeMode = ThemeMode.SYSTEM
    protected open val fontScale: Float = 1f
    protected open val mainFontResource: FontResource? = null
    protected open val background: Color? = null
    protected open val colorSystem: ColorSystem = ColorSystem.Default
    protected open val typographyTheme: TypographyTheme = TypographyTheme.Default
    protected open val shapeTheme: ShapeTheme = ShapeTheme.Default
    protected open val geometryTheme: GeometryTheme = GeometryTheme.Default
    protected open val animationTheme: AnimationTheme = AnimationTheme.Default
    protected open val toolingTheme: ToolingTheme = ToolingTheme.Default
    protected open val valueTheme: ValueTheme = ValueTheme.Default
    protected open val localProvider: Array<ProvidedValue<*>> = emptyArray()

    protected open fun onCreate() { }
    protected open suspend fun onCreateLater() { }
    protected open fun onDestroyBefore() { }
    protected open fun onDestroy() { }

    @Composable
    abstract fun Content()

    val context: Context = Context(delegate)

    @Composable
    fun ComposedLayout(
        modifier: Modifier = Modifier.fillMaxSize(),
        bgColor: Color? = null,
        content: @Composable () -> Unit = { Content() }
    ) {
        Theme(
            themeMode = themeMode,
            fontScale = fontScale,
            mainFontResource = mainFontResource,
            background = bgColor ?: background,
            colorSystem = colorSystem,
            typographyTheme = typographyTheme,
            shapeTheme = shapeTheme,
            geometryTheme = geometryTheme,
            animationTheme = animationTheme,
            toolingTheme = toolingTheme,
            valueTheme = valueTheme,
            modifier = modifier
        ) {
            CompositionLocalProvider(*localProvider, content = content)
        }
    }

    internal fun openService(scope: CoroutineScope) {
        @Suppress("UNCHECKED_CAST")
        self.init(this as A)
        initService(scope, context)
        onCreate()
    }

    internal suspend fun CoroutineScope.openServiceLater() {
        initServiceLater(context)
        onCreateLater()
    }

    internal fun closeServiceBefore() {
        destroyServiceBefore(context)
        onDestroyBefore()
    }

    internal fun closeService() {
        destroyService(context)
        onDestroy()
    }
}