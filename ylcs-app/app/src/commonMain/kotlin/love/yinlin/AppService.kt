package love.yinlin

import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Paths
import love.yinlin.common.Resource
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import love.yinlin.service.Service

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
}

expect val service: AppService