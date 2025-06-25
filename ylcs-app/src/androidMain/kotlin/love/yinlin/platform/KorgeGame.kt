package love.yinlin.platform

import android.content.Intent
import androidx.compose.runtime.Stable
import korlibs.korge.Korge
import korlibs.korge.KorgeDisplayMode
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Anchor2D
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.Size2D
import love.yinlin.AppModel
import love.yinlin.activity.KorgeActivity

@Stable
actual abstract class KorgeGame actual constructor(
    actual val appModel: AppModel,
    actual val appContext: AppContext
) {
    private val actualContext = appContext as ActualAppContext

    actual abstract val mainScene: Scene

    actual val korge: Korge = Korge(
        windowSize = Size2D(1280, 720),
        virtualSize = Size2D(1280, 720),
        displayMode = KorgeDisplayMode(ScaleMode.SHOW_ALL, Anchor2D.CENTER, false),
        main = {
            sceneContainer().changeTo { mainScene }
        }
    )

    actual suspend fun launch() {
        try {
            val activity = actualContext.activity!!
            val intent = Intent(activity, KorgeActivity::class.java)
            KorgeActivity.GlobalKorge = korge
            activity.startActivity(intent)
        }
        catch (_: Throwable) {
            KorgeActivity.GlobalKorge = null
        }
    }
}