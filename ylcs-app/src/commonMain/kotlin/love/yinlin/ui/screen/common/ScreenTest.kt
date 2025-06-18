package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.constrainedWithin
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.ui.component.screen.CommonSubScreen
import kotlin.random.Random


class Ball : Visible, Dynamic {
    private lateinit var viewportManager: ViewportManager
    private val radius = 40.sceneUnit
    override val body = BoxBody(
        initialPosition = SceneOffset(
            x = Random.nextInt(100).sceneUnit,
            y = Random.nextInt(100).sceneUnit
        ),
        initialSize = SceneSize(
            width = radius * 2,
            height = radius * 2,
        ),
    )

    private var horizontalSpeed = 0.5f.sceneUnit
    private var verticalSpeed = 0.5f.sceneUnit
    private var previousPosition = body.position

    override fun update(deltaTimeInMilliseconds: Int) {
        val viewportTopLeft = viewportManager.topLeft.value
        val viewportBottomRight = viewportManager.bottomRight.value
        val offset = SceneOffset(
            x = horizontalSpeed,
            y = verticalSpeed,
        )
        val nextPosition = (body.position + offset * deltaTimeInMilliseconds).constrainedWithin(
            topLeft = viewportTopLeft,
            bottomRight = viewportBottomRight,
        )
        var shouldJumpBackToPreviousPosition = false
        if (nextPosition.x == viewportTopLeft.x || nextPosition.x == viewportBottomRight.x) {
            shouldJumpBackToPreviousPosition = true
            horizontalSpeed *= -1
        }
        if (nextPosition.y == viewportTopLeft.y || nextPosition.y == viewportBottomRight.y) {
            shouldJumpBackToPreviousPosition = true
            verticalSpeed *= -1
        }
        if (shouldJumpBackToPreviousPosition) {
            body.position = previousPosition
        }
        previousPosition = body.position
        body.position = nextPosition
    }

    override fun DrawScope.draw() {
        drawCircle(
            color = Colors.Green,
            radius = radius.raw,
            center = body.pivot.raw,
        )
    }

    override fun onAdded(kubriko: Kubriko) {
        viewportManager = kubriko.get()
    }
}

class GameplayManager : Manager() {

    private val actorManager by manager<ActorManager>()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(Ball())
        actorManager.add(Ball())
    }
}

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    private val engine = Kubriko.newInstance(GameplayManager())

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {
        KubrikoViewport(
            kubriko = engine,
            modifier = Modifier.fillMaxSize()
        )
    }
}