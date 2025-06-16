package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.consumeAsFlow
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.data.rachel.sockets.LyricsSockets
import love.yinlin.extension.parseJsonValue
import love.yinlin.platform.app
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.screen.CommonSubScreen

private suspend inline fun DefaultClientWebSocketSession.send(data: LyricsSockets.CM) = this.sendSerialized(data)

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    private var session: DefaultClientWebSocketSession? = null

    override suspend fun initialize() {
        launch {
            try {
                app.socketsClient.webSocketSession(host = "api.yinlin.love", path = LyricsSockets.path).let {
                    session = it
                    it.incoming.consumeAsFlow().collect { frame ->
                        if (frame is Frame.Text) {
                            val msg = frame.readText().parseJsonValue<LyricsSockets.SM>()

                            println(msg)
                        }
                    }
                }
            }
            catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                session = null
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Column {
            app.config.userProfile?.let { profile ->
                LoadingRachelButton("连接") {
                    session?.send(LyricsSockets.CM.Login(LyricsSockets.PlayerInfo(profile.uid, profile.name)))
                }
            }
        }
    }
}