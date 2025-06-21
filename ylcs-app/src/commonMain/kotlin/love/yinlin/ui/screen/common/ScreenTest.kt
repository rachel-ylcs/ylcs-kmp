package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.rememberState
import love.yinlin.extension.toJsonString
import love.yinlin.ui.component.container.PaintCanvas
import love.yinlin.ui.component.container.PaintPath
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {
        val items = remember { mutableStateListOf<PaintPath>() }
        var text: String? by rememberState { null }
        var enabled: Boolean by rememberState { true }

        Column(modifier = Modifier.fillMaxSize()) {
            RachelButton("保存") {
                text = items.toList().toJsonString()
                println(text?.length ?: 0)
            }
            RachelButton("清除") {
                items.clear()
            }
            RachelButton("载入") {
                text?.parseJsonValue<List<PaintPath>>()?.let { items.addAll(it) }
            }
            RachelButton(if (enabled) "锁定" else "解锁") { enabled = !enabled }

            PaintCanvas(
                items = items,
                color = Colors.Black,
                width = 1f,
                enabled = enabled,
                onPathAdded = { items += it },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}