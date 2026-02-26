package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.xwwk
import love.yinlin.common.PathMod
import love.yinlin.compose.AnimationTheme
import love.yinlin.compose.DurationTheme
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.config.patches
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.screen.DeepLink
import love.yinlin.cs.ClientEngine
import love.yinlin.extension.DateEx
import love.yinlin.extension.mkdir
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.foundation.StartupDelegate
import love.yinlin.foundation.StartupLazyFetcher
import love.yinlin.foundation.StartupNative
import love.yinlin.foundation.useNotPlatformStartupLazyFetcher
import love.yinlin.foundation.usePlatformStartupLazyFetcher
import love.yinlin.platform.Platform
import love.yinlin.startup.StartupExceptionHandler
import love.yinlin.startup.StartupKV
import love.yinlin.startup.StartupOS
import love.yinlin.startup.StartupPicker
import love.yinlin.startup.StartupUrlImage
import org.jetbrains.compose.resources.FontResource

@Stable
abstract class AbstractRachelApplication(delegate: PlatformContextDelegate) : PlatformApplication<AbstractRachelApplication>(mApp, delegate), DeepLink {
    val os by service(
        Local.info.appName,
        priority = StartupDelegate.HIGH8,
        factory = ::StartupOS
    )

    private val createDirectories by sync {
        Platform.useNot(*Platform.Web) {
            os.storage.dataPath.mkdir()
            os.storage.cachePath.mkdir()
            PathMod.mkdir()
        }
    }

    private val initClientBaseUrl by sync {
        ClientEngine.init(Local.API_BASE_URL)
    }

    @StartupNative
    val picker by service(
        factory = ::StartupPicker
    )

    val urlImage by service(
        useNotPlatformStartupLazyFetcher(*Platform.Web) { os.storage.cachePath.parent!! },
        Platform.use(*Platform.Phone, ifTrue = 400, ifFalse = 1024),
        ImageQuality.Medium,
        factory = ::StartupUrlImage
    )

    @StartupNative
    val kv by service(
        usePlatformStartupLazyFetcher(*Platform.Desktop) { Path(os.storage.dataPath, "config") },
        factory = ::StartupKV
    )

    val config by service(
        StartupLazyFetcher { kv },
        Local.info.version,
        patches(),
        factory = ::StartupAppConfig,
    )

    val exceptionHandler by service(
        "crash_key",
        StartupExceptionHandler.Handler { key, e, error ->
            kv.set(key, "${DateEx.CurrentString}\n$error")
            println(e.stackTraceToString())
        },
        factory = ::StartupExceptionHandler
    )

    override val themeMode: ThemeMode get() = config.themeMode
    override val fontScale: Float get() = config.fontScale.value
    override val mainFontResource: FontResource = Res.font.xwwk
    override val animationTheme: AnimationTheme get() = AnimationTheme.Default.copy(duration = DurationTheme.Default.clone(config.animationSpeed.value))
    override val toolingTheme: ToolingTheme get() = ToolingTheme(enableBallonTip = config.enabledTip)
}