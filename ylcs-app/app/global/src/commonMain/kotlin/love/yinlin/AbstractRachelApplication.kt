package love.yinlin

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.AnimationTheme
import love.yinlin.compose.DurationTheme
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.ToolingTheme
import love.yinlin.compose.config.patches
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.window.DeepLink
import love.yinlin.cs.ClientEngine
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingNull
import love.yinlin.foundation.PlatformContext
import love.yinlin.foundation.StartupID
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem
import love.yinlin.platform.Platform
import love.yinlin.startup.*
import org.jetbrains.compose.resources.FontResource

@Stable
abstract class AbstractRachelApplication(context: PlatformContext) : PlatformApplication<AbstractRachelApplication>(mApp, context), DeepLink<ScreenManager> {
    val dataPath: File = PlatformFileSystem.dataPath(rawContext, Local.info.appName)
    val cachePath: File = PlatformFileSystem.cachePath(rawContext, Local.info.appName)
    val configPath: File = File(dataPath, "config")
    val modPath: File = File(dataPath, "mod")

    init {
        ClientEngine.init(Local.API_BASE_URL)

        async {
            dataPath.mkdir()
            cachePath.mkdir()
            modPath.mkdir()
        }
    }

    val cache by startup(StartupCache.Factory(cachePath))

    val picker by startup(StartupPickerFactory())

    val urlImage by startup(StartupUrlImage.Factory(
        cachePath = cachePath,
        maxCacheSize = Platform.use(*Platform.Phone, ifTrue = 400, ifFalse = 1024),
        imageQuality = ImageQuality.Medium)
    )

    val kv by startup(StartupKVFactory(configPath))

    val config by startup(StartupConfig.custom(Local.info.version, patches(), ::StartupAppConfig))

    val exceptionHandler by startup(StartupExceptionHandler.Factory("crash_key") { key, e, error ->
        kv.set(key, "${DateEx.CurrentString}\n${error}")
        println(e.stackTraceToString())
    }, dependencies = listOf(StartupID<StartupKV>()))

    override val themeMode: ThemeMode get() = config.themeMode
    override val fontScale: Float get() = config.fontScale.value
    override val mainFontResource: FontResource = Res.font.xwwk
    override val animationTheme: AnimationTheme get() = AnimationTheme.Default.copy(duration = DurationTheme.Default.clone(config.animationSpeed.value))
    override val toolingTheme: ToolingTheme get() = ToolingTheme(enableBallonTip = config.enabledTip)

    /**
     * 目录操作
     */
    suspend fun calcCacheSize(): Long = cachePath.parent?.size() ?: 0L

    suspend fun clearCache() {
        cachePath.deleteRecursively()
        cachePath.mkdir()
    }

    suspend inline fun createTempFile(filename: String? = null, crossinline block: suspend (Sink) -> Boolean): File? = catchingNull {
        val name = filename ?: DateEx.CurrentLong.toString()
        File(cachePath, name).apply { write { require(block(it)) } }
    }

    suspend fun createTempFolder(filename: String? = null): File? = catchingNull {
        val name = filename ?: DateEx.CurrentLong.toString()
        File(cachePath, name).apply { mkdir() }
    }
}