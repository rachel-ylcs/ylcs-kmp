package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsClientEngineConfig

actual object NetClient {
    private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
        engine {

        }
    }

    actual val common: HttpClient by lazy { HttpClient(Js) {
        useEngine()
        useJson()
        useCommonTimeout()
    } }

    actual val file: HttpClient by lazy { HttpClient(Js) {
        useEngine()
        useJson()
        useFileTimeout()
    } }

    actual val sockets: HttpClient by lazy { HttpClient(Js) {
        useWebSockets()
    } }
}