package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.app

data object Paths {
    val modPath: Path by lazy { Path(app.os.storage.dataPath, "mod") }
}