package love.yinlin.compose.screen

import androidx.compose.runtime.Stable
import love.yinlin.compose.LaunchFlag

@Stable
abstract class SubScreen(
    private val parent: BasicScreen<*>,

) {

    val firstLoad = LaunchFlag()
}