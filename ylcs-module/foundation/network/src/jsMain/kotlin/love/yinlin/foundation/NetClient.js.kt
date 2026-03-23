package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsClientEngineConfig

private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
    engine {

    }
}

actual fun buildCommonNetClient(timeout: Long): NetClient = NetClient(HttpClient(Js) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildFileClient(timeout: Long): NetClient = NetClient(HttpClient(Js) {
    useEngine()
    useJson()
    useTimeout(timeout)
})

actual fun buildSocketClient(): WebSocketClient = WebSocketClient(HttpClient(Js) {
    useWebSockets()
})