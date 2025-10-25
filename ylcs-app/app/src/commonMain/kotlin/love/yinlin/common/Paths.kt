package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.AppService

data object Paths {
    val musicPath: Path get() = Path(AppService.os.storage.dataPath, "music")
}