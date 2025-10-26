package love.yinlin

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Paths
import love.yinlin.common.Resource
import love.yinlin.compose.data.ImageQuality
import love.yinlin.extension.DateEx
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import love.yinlin.service.Service
import love.yinlin.service.StartupLazyFetcher
import love.yinlin.startup.StartupConfig
import love.yinlin.startup.StartupExceptionHandler
import love.yinlin.startup.StartupKV
import love.yinlin.startup.StartupUrlImage
import love.yinlin.startup.buildStartupExceptionHandler

abstract class AppService : Service(Local.info) {
    val createDirectories by sync {
        Platform.useNot(Platform.WebWasm) {
            SystemFileSystem.createDirectories(os.storage.dataPath)
            SystemFileSystem.createDirectories(os.storage.cachePath)
            SystemFileSystem.createDirectories(Paths.musicPath)
        }
    }

    val loadResources by free {
        Coroutines.io {
            Resource.initialize()
        }
    }

    val setupUrlImage by service(
        StartupLazyFetcher { os.storage.cachePath },
        Platform.use(*Platform.Phone, ifTrue = 400, ifFalse = 1024),
        ImageQuality.Medium,
        factory = ::StartupUrlImage
    )

    val kv by service(
        StartupLazyFetcher {
            Platform.use(*Platform.Desktop,
                ifTrue = { Path(os.storage.dataPath, "config") },
                ifFalse = { null }
            )
        },
        factory = ::StartupKV
    )

    val exceptionHandler by service(
        "crash_key",
        StartupExceptionHandler.Handler { key, e, error ->
            kv.set(key, "${DateEx.CurrentString}\n$error")
            println(e.stackTraceToString())
        },
        factory = ::buildStartupExceptionHandler
    )

    val config by service(
        StartupLazyFetcher { kv },
        factory = ::StartupConfig
    )
}

expect val service: AppService