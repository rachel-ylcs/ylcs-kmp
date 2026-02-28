package love.yinlin.common

import kotlinx.io.files.Path
import love.yinlin.app

val PathMod: Path by lazy { Path(app.os.storage.dataPath, "mod") }