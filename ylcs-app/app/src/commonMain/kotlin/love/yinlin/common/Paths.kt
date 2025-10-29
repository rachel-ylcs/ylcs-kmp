package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.app

data object Paths {
    val musicPath: Path get() = Path(app.os.storage.dataPath, "music")
}