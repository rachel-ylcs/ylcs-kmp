package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.service

data object Paths {
    val musicPath: Path get() = Path(service.os.storage.dataPath, "music")
}