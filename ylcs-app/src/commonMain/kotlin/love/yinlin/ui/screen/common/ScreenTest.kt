package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.ui.component.container.PaintCanvas
import love.yinlin.ui.component.container.PaintPath
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {
        val items = mutableStateListOf<PaintPath>()

        PaintCanvas(
            items = items,
            color = Colors.Black,
            width = 1f,
            onPathAdded = { items += it },
            modifier = Modifier.fillMaxSize()
        )
    }
}