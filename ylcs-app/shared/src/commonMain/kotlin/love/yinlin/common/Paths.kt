package love.yinlin.common

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.app

@Stable
object Paths {
    val modPath: Path by lazy { Path(app.os.storage.dataPath, "mod") }
}